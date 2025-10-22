package com.citi.audit_service.service;

import com.citi.audit_service.exception.AuditSerializationException;
import com.citi.audit_service.model.AuditEvent;
import com.citi.audit_service.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javers.core.Javers;
import org.javers.core.commit.Commit;
import org.javers.core.diff.Change;
import org.javers.core.diff.Diff;
import org.javers.core.diff.changetype.ValueChange;
import org.javers.core.metamodel.object.CdoSnapshot;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JaVers-integrated Audit Service that combines:
 * 1. JaVers for object versioning, snapshots and semantic diffs
 * 2. Custom audit event logging for business audit trail
 *
 * This service is marked @Primary to be injected into AuditAspect
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class JaversAuditService {

    private final Javers javers;
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Log entity creation event
     * - Commits snapshot to JaVers repository
     * - Logs audit event to custom audit table
     */
    @Transactional
    public void logEntityCreated(String domain, String entityType, Object entity, String initiator) {
        try {
            String author = initiator != null ? initiator : "SYSTEM";

            // 1. Commit snapshot to JaVers (creates version 1.0)
            Commit commit = javers.commit(author, entity);
            log.debug("JaVers committed CREATE snapshot for {} - Commit ID: {}", entityType, commit.getId());

            // 2. Log to custom audit table
            String payload = objectMapper.writeValueAsString(entity);
            String summary = String.format("%s created (JaVers Commit: %s)", entityType, commit.getId());

            logEvent(domain, entityType, getEntityId(entity), "CREATED", payload, summary, 1, initiator);

        } catch (JsonProcessingException e) {
            throw new AuditSerializationException("Failed to serialize " + entityType + " for audit", e);
        }
    }

    /**
     * Log entity update event with JaVers diff
     * - Computes semantic diff between old and new state
     * - Commits new snapshot to JaVers
     * - Logs detailed field changes to audit table
     */
    @Transactional
    public void logEntityUpdated(String domain, String entityType, Object oldEntity, Object newEntity, String initiator) {
        try {
            String author = initiator != null ? initiator : "SYSTEM";

            // 1. Compute semantic diff using JaVers
            Diff diff = javers.compare(oldEntity, newEntity);
            List<Change> changes = diff.getChanges();

            if (changes.isEmpty()) {
                log.debug("No changes detected for {} update", entityType);
                return;
            }

            // 2. Commit new snapshot to JaVers
            Commit commit = javers.commit(author, newEntity);
            log.debug("JaVers committed UPDATE snapshot for {} - Commit ID: {}, Changes: {}",
                     entityType, commit.getId(), changes.size());

            // 3. Build change payload with JaVers diff
            Map<String, Object> changePayload = new HashMap<>();
            changePayload.put("oldValue", oldEntity);
            changePayload.put("newValue", newEntity);
            changePayload.put("javersCommitId", commit.getId().toString());
            changePayload.put("changes", buildChangeMap(changes));

            String payload = objectMapper.writeValueAsString(changePayload);

            // 4. Generate detailed summary from JaVers changes
            String summary = generateJaversSummary(entityType, changes, commit.getId().toString());

            // 5. Log to custom audit table
            logEvent(domain, entityType, getEntityId(newEntity), "UPDATED", payload, summary,
                    getVersion(newEntity), initiator);

        } catch (JsonProcessingException e) {
            throw new AuditSerializationException("Failed to serialize " + entityType + " for audit", e);
        }
    }

    /**
     * Log entity deletion event
     * - Records shallow delete in JaVers
     * - Logs deletion to audit table
     */
    @Transactional
    public void logEntityDeleted(String domain, String entityType, Object deletedEntity, String initiator) {
        try {
            String author = initiator != null ? initiator : "SYSTEM";

            // 1. Commit shallow delete to JaVers (marks object as deleted)
            Commit commit = javers.commitShallowDelete(author, deletedEntity);
            log.debug("JaVers committed DELETE snapshot for {} - Commit ID: {}", entityType, commit.getId());

            // 2. Log to custom audit table
            String payload = objectMapper.writeValueAsString(deletedEntity);
            String summary = String.format("%s deleted (JaVers Commit: %s)", entityType, commit.getId());

            logEvent(domain, entityType, getEntityId(deletedEntity), "DELETED", payload, summary,
                    getVersion(deletedEntity), initiator);

        } catch (JsonProcessingException e) {
            throw new AuditSerializationException("Failed to serialize " + entityType + " for audit", e);
        }
    }

    /**
     * Generate human-readable summary from JaVers changes
     */
    private String generateJaversSummary(String entityType, List<Change> changes, String commitId) {
        if (changes.isEmpty()) {
            return entityType + " updated - no changes detected";
        }

        StringBuilder summary = new StringBuilder(entityType + " updated: ");
        List<String> fieldChanges = changes.stream()
                .filter(change -> change instanceof ValueChange)
                .map(change -> {
                    ValueChange valueChange = (ValueChange) change;
                    String propertyName = valueChange.getPropertyName();
                    Object left = valueChange.getLeft();
                    Object right = valueChange.getRight();

                    // Skip metadata fields
                    if (!propertyName.equals("id") && !propertyName.equals("updatedTimestamp")
                        && !propertyName.equals("createdTimestamp")) {
                        return String.format("%s (from '%s' to '%s')", propertyName, left, right);
                    }
                    return null;
                })
                .filter(s -> s != null)
                .collect(Collectors.toList());

        if (fieldChanges.isEmpty()) {
            return entityType + " updated - metadata only (JaVers Commit: " + commitId + ")";
        }

        summary.append(String.join(", ", fieldChanges));
        summary.append(" (JaVers Commit: ").append(commitId).append(")");
        return summary.toString();
    }

    /**
     * Build a map of changes from JaVers Change objects
     */
    private Map<String, Map<String, Object>> buildChangeMap(List<Change> changes) {
        Map<String, Map<String, Object>> changeMap = new HashMap<>();

        changes.stream()
                .filter(change -> change instanceof ValueChange)
                .forEach(change -> {
                    ValueChange valueChange = (ValueChange) change;
                    Map<String, Object> changeDetail = new HashMap<>();
                    changeDetail.put("old", valueChange.getLeft());
                    changeDetail.put("new", valueChange.getRight());
                    changeMap.put(valueChange.getPropertyName(), changeDetail);
                });

        return changeMap;
    }

    /**
     * Query JaVers snapshots for an entity
     */
    public List<CdoSnapshot> getEntitySnapshots(Class<?> entityClass, Object entityId) {
        return javers.findSnapshots(
                org.javers.repository.jql.QueryBuilder.byInstanceId(entityId, entityClass).build()
        );
    }

    /**
     * Get changes for a specific entity from JaVers
     */
    public List<Change> getEntityChanges(Class<?> entityClass, Object entityId) {
        return javers.findChanges(
                org.javers.repository.jql.QueryBuilder.byInstanceId(entityId, entityClass).build()
        );
    }

    /**
     * Internal method to log audit event to custom table
     */
    private void logEvent(String domain, String entity, Long entityId, String eventName,
                         String payload, String summary, Integer version, String initiator) {
        AuditEvent event = new AuditEvent();
        event.setEventDomain(domain);
        event.setEventEntity(entity);
        event.setEventEntityId(entityId);
        event.setEventName(eventName);
        event.setEventPayload(payload);
        event.setEventSummary(summary);
        event.setEventEntityVersion(version);
        event.setEventTimestamp(LocalDateTime.now());
        event.setEntryTimestamp(LocalDateTime.now());
        event.setEventInitiator(initiator);
        auditEventRepository.save(event);
    }

    /**
     * Extract entity ID using reflection
     */
    private Long getEntityId(Object obj) {
        try {
            return (Long) obj.getClass().getMethod("getId").invoke(obj);
        } catch (Exception e) {
            log.warn("Failed to extract ID from {}", obj.getClass().getSimpleName());
            return null;
        }
    }

    /**
     * Get version number from entity
     */
    private Integer getVersion(Object obj) {
        try {
            return (Integer) obj.getClass().getMethod("getVersion").invoke(obj);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     * Get all audit events from custom audit table
     */
    public List<AuditEvent> getAllAuditEvents() {
        return auditEventRepository.findAll();
    }
}


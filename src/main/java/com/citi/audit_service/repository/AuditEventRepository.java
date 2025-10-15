package com.citi.audit_service.repository;

import com.citi.audit_service.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    // Find all audit events by event name (e.g., "CREATED", "UPDATED", "DELETED")
    List<AuditEvent> findByEventName(String eventName);

    // Find audit events by entity and entity ID
    List<AuditEvent> findByEventEntityAndEventEntityId(String eventEntity, Long entityId);

    // Find audit events by initiator
    List<AuditEvent> findByEventInitiator(String eventInitiator);

    // Find audit events within a date range
    List<AuditEvent> findByEventTimestampBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Find audit events by entity ID ordered by timestamp descending
    List<AuditEvent> findByEventEntityIdOrderByEventTimestampDesc(Long entityId);

    // Custom query to find recent audit events
    @Query("SELECT a FROM AuditEvent a WHERE a.eventEntity = :entity ORDER BY a.eventTimestamp DESC")
    List<AuditEvent> findRecentAuditEvents(@Param("entity") String entity);
}
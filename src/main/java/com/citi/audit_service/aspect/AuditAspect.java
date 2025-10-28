package com.citi.audit_service.aspect;

import com.citi.audit_service.annotation.AuditAction;
import com.citi.audit_service.annotation.Auditable;
import com.citi.audit_service.service.JaversAuditService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * Generic AOP Aspect for auditing operations on any entity.
 * This aspect intercepts methods annotated with @Auditable and logs audit events.
 * Works with Employee, Department, Address, Training, or any other entity.
 *
 * Now integrated with JaVers for:
 * - Object versioning and snapshot storage
 * - Semantic diff computation
 * - Change history tracking
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final JaversAuditService javersAuditService;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    // ThreadLocal to store old entity state for UPDATE/DELETE operations
    // Stores a deep copy to ensure changes are properly detected
    private final ThreadLocal<Object> oldEntityState = new ThreadLocal<>();

    /**
     * Intercept methods before execution to capture the old state for UPDATE/DELETE operations
     */
    @Before("@annotation(auditable)")
    public void beforeAuditableMethod(JoinPoint joinPoint, Auditable auditable) {
        if (auditable.action() == AuditAction.UPDATE || auditable.action() == AuditAction.DELETE) {
            Object[] args = joinPoint.getArgs();

            // For UPDATE/DELETE: first argument is typically the ID
            if (args.length > 0 && args[0] instanceof Long) {
                Long entityId = (Long) args[0];
                Object target = joinPoint.getTarget();

                try {
                    // Dynamically find and call the get method (e.g., getEmployeeById, getDepartmentById)
                    Object currentEntity = findEntityById(target, entityId, auditable.entity());

                    if (currentEntity != null) {
                        // Create a DEEP COPY of the entity to avoid reference issues
                        Object deepCopy = createDeepCopy(currentEntity);

                        // Store snapshot of current state
                        oldEntityState.set(deepCopy);
                        log.debug("Captured old state for {} with ID: {} (for JaVers diff)", auditable.entity(), entityId);
                    }
                } catch (Exception e) {
                    log.error("Failed to capture old {} state for audit", auditable.entity(), e);
                }
            }
        }
    }

    /**
     * Intercept methods after successful execution to log audit events
     * Now uses JaVers for snapshot storage and diff computation
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void afterAuditableMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();

            String initiator = extractInitiator(args, result);
            String domain = auditable.domain();
            String entity = auditable.entity();

            switch (auditable.action()) {
                case CREATE:
                    handleCreate(result, domain, entity, initiator);
                    break;
                case UPDATE:
                    handleUpdate(result, domain, entity, initiator);
                    break;
                case DELETE:
                    handleDelete(domain, entity, initiator);
                    break;
            }

        } catch (Exception e) {
            log.error("Failed to log audit event for {}", auditable.entity(), e);
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            oldEntityState.remove();
        }
    }

    /**
     * Handle CREATE audit event - uses JaVers to commit snapshot
     */
    private void handleCreate(Object result, String domain, String entity, String initiator) {
        if (result != null) {
            javersAuditService.logEntityCreated(domain, entity, result, initiator);
            log.debug("Audit logged with JaVers: {} created - ID: {}", entity, getEntityId(result));
        }
    }

    /**
     * Handle UPDATE audit event - uses JaVers to compute diff and commit snapshot
     */
    private void handleUpdate(Object result, String domain, String entity, String initiator) {
        if (result != null) {
            Object oldEntity = oldEntityState.get();

            if (oldEntity != null) {
                javersAuditService.logEntityUpdated(domain, entity, oldEntity, result, initiator);
                log.debug("Audit logged with JaVers: {} updated - ID: {}", entity, getEntityId(result));
            } else {
                log.warn("Old {} state not found for UPDATE audit", entity);
            }
        }
    }

    /**
     * Handle DELETE audit event - uses JaVers to commit shallow delete
     */
    private void handleDelete(String domain, String entity, String initiator) {
        Object deletedEntity = oldEntityState.get();

        if (deletedEntity != null) {
            javersAuditService.logEntityDeleted(domain, entity, deletedEntity, initiator);
            log.debug("Audit logged with JaVers: {} deleted - ID: {}", entity, getEntityId(deletedEntity));
        } else {
            log.warn("Old {} state not found for DELETE audit", entity);
        }
    }

    /**
     * Dynamically find entity by ID using reflection
     * Supports methods like: getEmployeeById, getDepartmentById, getAddressById, etc.
     */
    private Object findEntityById(Object service, Long entityId, String entityType) {
        try {
            // Try common method naming patterns
            String[] methodPatterns = {
                "get" + entityType.substring(0, 1).toUpperCase() + entityType.substring(1).toLowerCase() + "ById",
                "getById",
                "findById"
            };

            for (String methodName : methodPatterns) {
                try {
                    var method = service.getClass().getMethod(methodName, Long.class);
                    Object result = method.invoke(service, entityId);

                    // Handle Optional return type
                    if (result != null && result.getClass().getName().contains("Optional")) {
                        var optionalGetMethod = result.getClass().getMethod("orElse", Object.class);
                        return optionalGetMethod.invoke(result, (Object) null);
                    }

                    return result;
                } catch (NoSuchMethodException e) {
                    // Try next pattern
                    continue;
                }
            }
        } catch (Exception e) {
            log.error("Failed to find {} by ID: {}", entityType, entityId, e);
        }
        return null;
    }

    /**
     * Extract initiator from method arguments or result using reflection
     */
    private String extractInitiator(Object[] args, Object result) {
        // Try to get 'updatedBy' field from any argument
        for (Object arg : args) {
            if (arg != null) {
                String updatedBy = getUpdatedBy(arg);
                if (updatedBy != null) {
                    return updatedBy;
                }
            }
        }

        // Try to get initiator from result
        if (result != null) {
            String updatedBy = getUpdatedBy(result);
            if (updatedBy != null) {
                return updatedBy;
            }
        }

        // Default to SYSTEM if no initiator found
        return "SYSTEM";
    }

    /**
     * Get 'updatedBy' field from an object using reflection
     */
    private String getUpdatedBy(Object obj) {
        try {
            var method = obj.getClass().getMethod("getUpdatedBy");
            Object result = method.invoke(obj);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get entity ID from any object using reflection
     */
    private Long getEntityId(Object obj) {
        try {
            var method = obj.getClass().getMethod("getId");
            return (Long) method.invoke(obj);
        } catch (Exception e) {
            log.warn("Failed to extract ID from object", e);
            return null;
        }
    }

    /**
     * Create a deep copy of an entity using JSON serialization/deserialization
     * This ensures the old state is truly independent from the new state
     */
    private Object createDeepCopy(Object entity) {
        try {
            // Serialize to JSON string
            String json = objectMapper.writeValueAsString(entity);

            // Deserialize back to object (creates a new instance)
            return objectMapper.readValue(json, entity.getClass());
        } catch (Exception e) {
            log.error("Failed to create deep copy of entity, falling back to original reference", e);
            return entity; // Fallback to original (not ideal but prevents crashes)
        }
    }
}

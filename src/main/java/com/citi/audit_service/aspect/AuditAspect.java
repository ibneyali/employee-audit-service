package com.citi.audit_service.aspect;

import com.citi.audit_service.annotation.AuditAction;
import com.citi.audit_service.annotation.Auditable;
import com.citi.audit_service.model.Employee;
import com.citi.audit_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

/**
 * AOP Aspect for auditing employee operations.
 * This aspect intercepts methods annotated with @Auditable and logs audit events.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;

    // ThreadLocal to store old employee state for UPDATE operations
    private final ThreadLocal<Employee> oldEmployeeState = new ThreadLocal<>();

    /**
     * Intercept methods before execution to capture the old state for UPDATE operations
     */
    @Before("@annotation(auditable)")
    public void beforeAuditableMethod(JoinPoint joinPoint, Auditable auditable) {
        if (auditable.action() == AuditAction.UPDATE || auditable.action() == AuditAction.DELETE) {
            Object[] args = joinPoint.getArgs();

            // For UPDATE: first argument is ID, second is the new employee details
            // For DELETE: first argument is ID
            if (args.length > 0 && args[0] instanceof Long) {
                Long employeeId = (Long) args[0];

                // Get the target object (EmployeeService instance)
                Object target = joinPoint.getTarget();

                try {
                    // Use reflection to call getEmployeeById to get current state
                    var method = target.getClass().getMethod("getEmployeeById", Long.class);
                    var optionalEmployee = method.invoke(target, employeeId);

                    if (optionalEmployee != null) {
                        var getMethod = optionalEmployee.getClass().getMethod("get");
                        Employee currentEmployee = (Employee) getMethod.invoke(optionalEmployee);

                        // Create a snapshot of the current state
                        Employee snapshot = Employee.builder()
                                .id(currentEmployee.getId())
                                .firstName(currentEmployee.getFirstName())
                                .lastName(currentEmployee.getLastName())
                                .email(currentEmployee.getEmail())
                                .phone(currentEmployee.getPhone())
                                .departmentId(currentEmployee.getDepartmentId())
                                .addressId(currentEmployee.getAddressId())
                                .createdTimestamp(currentEmployee.getCreatedTimestamp())
                                .updatedTimestamp(currentEmployee.getUpdatedTimestamp())
                                .updatedBy(currentEmployee.getUpdatedBy())
                                .version(currentEmployee.getVersion())
                                .build();

                        oldEmployeeState.set(snapshot);
                    }
                } catch (Exception e) {
                    log.error("Failed to capture old employee state for audit", e);
                }
            }
        }
    }

    /**
     * Intercept methods after successful execution to log audit events
     */
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void afterAuditableMethod(JoinPoint joinPoint, Auditable auditable, Object result) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Object[] args = joinPoint.getArgs();

            String initiator = extractInitiator(args, result);

            switch (auditable.action()) {
                case CREATE:
                    handleCreate(result, initiator);
                    break;
                case UPDATE:
                    handleUpdate(result, initiator);
                    break;
                case DELETE:
                    handleDelete(initiator);
                    break;
            }
        } catch (Exception e) {
            log.error("Failed to log audit event", e);
        } finally {
            // Clean up ThreadLocal to prevent memory leaks
            oldEmployeeState.remove();
        }
    }

    /**
     * Handle CREATE audit event
     */
    private void handleCreate(Object result, String initiator) {
        if (result instanceof Employee savedEmployee) {
            auditService.logEmployeeCreated(savedEmployee, initiator);
            log.debug("Audit logged: Employee created - ID: {}", savedEmployee.getId());
        }
    }

    /**
     * Handle UPDATE audit event
     */
    private void handleUpdate(Object result, String initiator) {
        if (result instanceof Employee) {
            Employee updatedEmployee = (Employee) result;
            Employee oldEmployee = oldEmployeeState.get();

            if (oldEmployee != null) {
                auditService.logEmployeeUpdated(oldEmployee, updatedEmployee, initiator);
                log.debug("Audit logged: Employee updated - ID: {}", updatedEmployee.getId());
            }
        }
    }

    /**
     * Handle DELETE audit event
     */
    private void handleDelete(String initiator) {
        Employee deletedEmployee = oldEmployeeState.get();

        if (deletedEmployee != null) {
            auditService.logEmployeeDeleted(deletedEmployee, initiator);
            log.debug("Audit logged: Employee deleted - ID: {}", deletedEmployee.getId());
        }
    }

    /**
     * Extract initiator from method arguments or result
     */
    private String extractInitiator(Object[] args, Object result) {
        // Try to get initiator from Employee object in arguments
        for (Object arg : args) {
            if (arg instanceof Employee) {
                Employee emp = (Employee) arg;
                if (emp.getUpdatedBy() != null) {
                    return emp.getUpdatedBy();
                }
            }
        }

        // Try to get initiator from result
        if (result instanceof Employee) {
            Employee emp = (Employee) result;
            if (emp.getUpdatedBy() != null) {
                return emp.getUpdatedBy();
            }
        }

        // Default to SYSTEM if no initiator found
        return "SYSTEM";
    }
}


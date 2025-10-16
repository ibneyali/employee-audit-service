package com.citi.audit_service.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark methods that should be audited.
 * The AOP aspect will intercept methods with this annotation and log audit events.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    /**
     * The type of audit operation (CREATE, UPDATE, DELETE)
     */
    AuditAction action();

    /**
     * The entity domain (e.g., "HR")
     */
    String domain() default "HR";

    /**
     * The entity type (e.g., "EMPLOYEE")
     */
    String entity() default "EMPLOYEE";
}


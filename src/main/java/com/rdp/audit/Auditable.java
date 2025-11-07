package com.rdp.audit;

import java.lang.annotation.*;

/**
 * Custom annotation to mark methods for auditing
 * Provides more granular control over audit behavior
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    
    /**
     * Entity type for auditing (optional - can be inferred)
     */
    String entityType() default "";
    
    /**
     * Action type for auditing
     */
    AuditAction action() default AuditAction.VIEW;
    
    /**
     * Description of the operation
     */
    String description() default "";
    
    /**
     * Whether to capture old values (for updates)
     */
    boolean captureOldValues() default false;
    
    /**
     * Whether to capture new values (for creates/updates)
     */
    boolean captureNewValues() default true;
    
    /**
     * Fields to exclude from auditing
     */
    String[] excludeFields() default {"password", "passwordHash", "token"};
}
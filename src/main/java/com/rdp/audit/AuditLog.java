package com.rdp.audit;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity_type", columnList = "entity_type"),
    @Index(name = "idx_audit_entity_id", columnList = "entity_id"),
    @Index(name = "idx_audit_action", columnList = "action"),
    @Index(name = "idx_audit_user", columnList = "performed_by"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType; // Product, Customer, Category, etc.
    
    @Column(name = "entity_id", nullable = false)
    private String entityId; // ID of the modified entity
    
    @Column(name = "action", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private AuditAction action; // CREATE, UPDATE, DELETE, VIEW, EXPORT
    
    @Column(name = "performed_by", length = 100)
    private String performedBy; // Username who performed the action
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "request_uri", length = 500)
    private String requestUri;
    
    @Column(name = "http_method", length = 10)
    private String httpMethod;
    
    @Column(name = "old_values", columnDefinition = "TEXT")
    private String oldValues; // JSON format
    
    @Column(name = "new_values", columnDefinition = "TEXT")
    private String newValues; // JSON format
    
    @Column(name = "changed_fields", columnDefinition = "TEXT")
    private String changedFields; // Comma-separated field names
    
    @Column(name = "operation_description", length = 500)
    private String operationDescription;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo; // JSON for extra metadata
}
package com.rdp.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    
    public void logAction(String entityType, String entityId, AuditAction action, 
                         Object oldValue, Object newValue, HttpServletRequest request) {
        try {
            String username = getCurrentUsername();
            
            AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .performedBy(username)
                .timestamp(LocalDateTime.now())
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .httpMethod(request.getMethod())
                .sessionId(request.getSession().getId())
                .build();
            
            // Convert objects to JSON
            if (oldValue != null) {
                auditLog.setOldValues(convertToJson(oldValue));
            }
            if (newValue != null) {
                auditLog.setNewValues(convertToJson(newValue));
            }
            
            // Detect changed fields
            if (oldValue != null && newValue != null) {
                auditLog.setChangedFields(detectChangedFields(oldValue, newValue));
            }
            
            auditLog.setOperationDescription(
                String.format("%s operation on %s with ID: %s", action, entityType, entityId)
            );
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log audit action: {}", e.getMessage(), e);
        }
    }
    
    public void logAction(String entityType, String entityId, AuditAction action, HttpServletRequest request) {
        logAction(entityType, entityId, action, null, null, request);
    }
    
    public void logBulkAction(String entityType, AuditAction action, int count, HttpServletRequest request) {
        try {
            String username = getCurrentUsername();
            
            AuditLog auditLog = AuditLog.builder()
                .entityType(entityType)
                .entityId("BULK_" + count)
                .action(action)
                .performedBy(username)
                .timestamp(LocalDateTime.now())
                .ipAddress(getClientIpAddress(request))
                .userAgent(request.getHeader("User-Agent"))
                .requestUri(request.getRequestURI())
                .httpMethod(request.getMethod())
                .sessionId(request.getSession().getId())
                .operationDescription(String.format("Bulk %s operation on %d %s records", action, count, entityType))
                .build();
            
            auditLogRepository.save(auditLog);
            
        } catch (Exception e) {
            log.error("Failed to log bulk audit action: {}", e.getMessage(), e);
        }
    }
    
    private String getCurrentUsername() {
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null ? authentication.getName() : "SYSTEM";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private String convertToJson(Object object) {
        try {
            if (object == null) {
                return null;
            }
            
            // Create a specialized ObjectMapper for audit logging
            ObjectMapper auditMapper = new ObjectMapper();
            
            // Handle Hibernate lazy loading issues
            auditMapper.configure(com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
            auditMapper.configure(com.fasterxml.jackson.databind.MapperFeature.DEFAULT_VIEW_INCLUSION, false);
            
            // Register Hibernate module to handle lazy loading
            auditMapper.registerModule(new com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module());
            
            // Configure to ignore lazy-loaded properties that aren't initialized
            com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module hibernateModule = 
                new com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module();
            hibernateModule.disable(com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module.Feature.USE_TRANSIENT_ANNOTATION);
            hibernateModule.enable(com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module.Feature.SERIALIZE_IDENTIFIER_FOR_LAZY_NOT_LOADED_OBJECTS);
            auditMapper.registerModule(hibernateModule);
            
            return auditMapper.writeValueAsString(object);
            
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert object to JSON: {}", e.getMessage());
            // Fallback to a simpler representation
            try {
                return createSimpleRepresentation(object);
            } catch (Exception fallbackError) {
                log.error("Even fallback JSON conversion failed: {}", fallbackError.getMessage());
                return object.getClass().getSimpleName() + "@" + object.hashCode();
            }
        } catch (Exception e) {
            log.error("Unexpected error during JSON conversion: {}", e.getMessage());
            return object.getClass().getSimpleName() + "@" + object.hashCode();
        }
    }
    
    private String createSimpleRepresentation(Object object) {
        try {
            // Use reflection to get field values without triggering lazy loading
            StringBuilder sb = new StringBuilder();
            sb.append("{");
            
            java.lang.reflect.Field[] fields = object.getClass().getDeclaredFields();
            boolean first = true;
            
            for (java.lang.reflect.Field field : fields) {
                // Skip static and transient fields
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || 
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                
                field.setAccessible(true);
                Object value = field.get(object);
                
                // Only include simple values and avoid collections/associations
                if (value != null && isSimpleType(value.getClass())) {
                    if (!first) sb.append(", ");
                    sb.append("\"").append(field.getName()).append("\": ");
                    if (value instanceof String) {
                        sb.append("\"").append(value.toString().replace("\"", "\\\"")).append("\"");
                    } else {
                        sb.append(value.toString());
                    }
                    first = false;
                }
            }
            
            sb.append("}");
            return sb.toString();
            
        } catch (Exception e) {
            log.error("Failed to create simple representation: {}", e.getMessage());
            return object.toString();
        }
    }
    
    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() || 
               clazz.equals(String.class) ||
               clazz.equals(Boolean.class) ||
               clazz.equals(Integer.class) ||
               clazz.equals(Long.class) ||
               clazz.equals(Double.class) ||
               clazz.equals(Float.class) ||
               clazz.equals(java.time.LocalDateTime.class) ||
               clazz.equals(java.time.LocalDate.class) ||
               clazz.equals(java.math.BigDecimal.class) ||
               Number.class.isAssignableFrom(clazz);
    }
    
    private String detectChangedFields(Object oldValue, Object newValue) {
        // Implementation to detect changed fields
        // This would use reflection or Jackson to compare objects
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> oldMap = objectMapper.convertValue(oldValue, Map.class);
            @SuppressWarnings("unchecked")
            Map<String, Object> newMap = objectMapper.convertValue(newValue, Map.class);
            
            StringBuilder changedFields = new StringBuilder();
            for (String key : newMap.keySet()) {
                if (!java.util.Objects.equals(oldMap.get(key), newMap.get(key))) {
                    if (changedFields.length() > 0) {
                        changedFields.append(", ");
                    }
                    changedFields.append(key);
                }
            }
            return changedFields.toString();
        } catch (Exception e) {
            log.warn("Failed to detect changed fields: {}", e.getMessage());
            return "UNKNOWN";
        }
    }
    
    public Page<AuditLog> searchAuditLogs(String entityType, String entityId, AuditAction action, 
                                         String performedBy, LocalDateTime startDate, LocalDateTime endDate, 
                                         Pageable pageable) {
        
        // Use JPA Specification for dynamic filtering
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();
        
        // Add entity type filter
        if (entityType != null && !entityType.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("entityType"), entityType.trim()));
        }
        
        // Add entity ID filter
        if (entityId != null && !entityId.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("entityId"), entityId.trim()));
        }
        
        // Add action filter
        if (action != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("action"), action));
        }
        
        // Add user filter (case-insensitive contains)
        if (performedBy != null && !performedBy.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.upper(root.get("performedBy")), 
                       "%" + performedBy.trim().toUpperCase() + "%"));
        }
        
        // Add date range filter
        if (startDate != null && endDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.between(root.get("timestamp"), startDate, endDate));
        } else if (startDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
        } else if (endDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
        }
        
        // Always order by timestamp descending
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        
        return auditLogRepository.findAll(spec, sortedPageable);
    }
    
    public java.util.List<AuditLog> exportAuditLogs(String entityType, String entityId, 
                                                    AuditAction action, String performedBy, 
                                                    LocalDateTime startDate, LocalDateTime endDate) {
        
        Specification<AuditLog> spec = (root, query, cb) -> cb.conjunction();
        
        // Add filters (same logic as search but return List instead of Page)
        if (entityType != null && !entityType.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("entityType"), entityType.trim()));
        }
        
        if (entityId != null && !entityId.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("entityId"), entityId.trim()));
        }
        
        if (action != null) {
            spec = spec.and((root, query, cb) -> 
                cb.equal(root.get("action"), action));
        }
        
        if (performedBy != null && !performedBy.trim().isEmpty()) {
            spec = spec.and((root, query, cb) -> 
                cb.like(cb.upper(root.get("performedBy")), 
                       "%" + performedBy.trim().toUpperCase() + "%"));
        }
        
        if (startDate != null && endDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.between(root.get("timestamp"), startDate, endDate));
        } else if (startDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
        } else if (endDate != null) {
            spec = spec.and((root, query, cb) -> 
                cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
        }
        
        // Order by timestamp descending and return all matching records
        Sort sort = Sort.by(Sort.Direction.DESC, "timestamp");
        
        return auditLogRepository.findAll(spec, sort);
    }
}
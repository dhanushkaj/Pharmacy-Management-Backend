package com.rdp.audit;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;


@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditService auditService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @PersistenceContext
    private EntityManager entityManager;

    // Audit all controller methods in specific packages
    @After("execution(* com.rdp.controller.*Controller.create*(..)) && args(request,..)")
    public void auditCreate(JoinPoint joinPoint, Object request) {
        try {
            String entityType = extractEntityType(joinPoint);
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            log.debug("AuditAspect CREATE entityType={}", entityType);
            
            if (httpRequest != null) {
                // For CREATE, oldValue is null and newValue is the request data
                auditService.logAction(entityType, "NEW", AuditAction.CREATE, null, request, httpRequest);
            }
        } catch (Exception e) {
            log.error("Error in audit aspect for create: {}", e.getMessage());
        }
    }

    @Around("execution(* com.rdp.controller.*Controller.update*(..)) && args(id, request,..)")
    public Object auditUpdate(ProceedingJoinPoint joinPoint, Object id, Object request) throws Throwable {
        String entityType = extractEntityType(joinPoint);
        HttpServletRequest httpRequest = getCurrentHttpRequest();
        Object oldValue = null;
        Object result = null;
        log.debug("AuditAspect UPDATE start entityType={} id={}", entityType, id);
        
        try {
            // Capture old value before the update
            oldValue = findEntityById(entityType, id);
            
            // Proceed with the actual update
            result = joinPoint.proceed();
            
            // Log the audit after successful update
            if (httpRequest != null) {
                auditService.logAction(entityType, String.valueOf(id), AuditAction.UPDATE, oldValue, request, httpRequest);
            }
            
        } catch (Exception e) {
            log.error("Error in audit aspect for update: {}", e.getMessage());
            // Still throw the original exception
            throw e;
        }
        
        return result;
    }

    @After("execution(* com.rdp.controller.*Controller.delete*(..)) && args(id,..)")
    public void auditDelete(JoinPoint joinPoint, Object id) {
        try {
            String entityType = extractEntityType(joinPoint);
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            log.debug("AuditAspect DELETE entityType={} id={}", entityType, id);
            
            if (httpRequest != null) {
                auditService.logAction(entityType, String.valueOf(id), AuditAction.DELETE, httpRequest);
            }
        } catch (Exception e) {
            log.error("Error in audit aspect for delete: {}", e.getMessage());
        }
    }

    @After("execution(* com.rdp.controller.*Controller.findAll*(..)) || execution(* com.rdp.controller.*Controller.all*(..))")
    public void auditViewAll(JoinPoint joinPoint) {
        try {
            String entityType = extractEntityType(joinPoint);
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            log.trace("AuditAspect VIEW ALL entityType={}", entityType);
            
            if (httpRequest != null) {
                auditService.logAction(entityType, "ALL", AuditAction.VIEW, httpRequest);
            }
        } catch (Exception e) {
            log.error("Error in audit aspect for view all: {}", e.getMessage());
        }
    }

    @After("execution(* com.rdp.controller.*Controller.findById*(..)) && args(id,..)")
    public void auditViewById(JoinPoint joinPoint, Object id) {
        try {
            String entityType = extractEntityType(joinPoint);
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            log.trace("AuditAspect VIEW BY ID entityType={} id={}", entityType, id);
            
            if (httpRequest != null) {
                auditService.logAction(entityType, String.valueOf(id), AuditAction.VIEW, httpRequest);
            }
        } catch (Exception e) {
            log.error("Error in audit aspect for view by id: {}", e.getMessage());
        }
    }

    @After("execution(* com.rdp.controller.*Controller.bulk*(..)) && args(items,..)")
    public void auditBulkOperation(JoinPoint joinPoint, java.util.List<?> items) {
        try {
            String entityType = extractEntityType(joinPoint);
            HttpServletRequest httpRequest = getCurrentHttpRequest();
            String methodName = joinPoint.getSignature().getName().toLowerCase();
            log.debug("AuditAspect BULK entityType={} size={} method={} ", entityType, (items==null?0:items.size()), methodName);
            
            AuditAction action = AuditAction.BULK_UPDATE;
            if (methodName.contains("delete")) {
                action = AuditAction.BULK_DELETE;
            } else if (methodName.contains("create") || methodName.contains("import")) {
                action = AuditAction.IMPORT;
            }
            
            if (httpRequest != null && items != null) {
                auditService.logBulkAction(entityType, action, items.size(), httpRequest);
            }
        } catch (Exception e) {
            log.error("Error in audit aspect for bulk operation: {}", e.getMessage());
        }
    }

    private String extractEntityType(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        // Remove "Controller" suffix and return entity name
        return className.replace("Controller", "");
    }

    private HttpServletRequest getCurrentHttpRequest() {
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            return attributes.getRequest();
        } catch (Exception e) {
            log.warn("Could not get current HTTP request: {}", e.getMessage());
            return null;
        }
    }
    
    private Object findEntityById(String entityType, Object id) {
        try {
            // Convert id to Long if it's a string
            Long entityId = null;
            if (id instanceof String) {
                entityId = Long.parseLong((String) id);
            } else if (id instanceof Long) {
                entityId = (Long) id;
            } else if (id instanceof Number) {
                entityId = ((Number) id).longValue();
            }
            
            if (entityId == null) {
                return null;
            }
            
            // Try to find the appropriate repository bean and fetch the entity
            String repositoryBeanName = entityType.toLowerCase() + "Repository";
            
            try {
                @SuppressWarnings("unchecked")
                JpaRepository<Object, Long> repository = (JpaRepository<Object, Long>) applicationContext.getBean(repositoryBeanName);
                return repository.findById(entityId).orElse(null);
            } catch (Exception e) {
                log.debug("Could not find repository bean '{}' or entity with ID {}: {}", repositoryBeanName, entityId, e.getMessage());
                return null;
            }
            
        } catch (Exception e) {
            log.error("Error finding entity '{}' with ID {}: {}", entityType, id, e.getMessage());
            return null;
        }
    }
}
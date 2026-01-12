package com.rdp.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class AuditTrailService {
    public void logAction(String userId, String action, String details, LocalDateTime timestamp, String originalValue, String modifiedValue) {
        // TODO: Persist audit log to database or external system
        System.out.printf("AUDIT | user=%s | action=%s | details=%s | time=%s | original=%s | modified=%s\n",
                userId, action, details, timestamp, originalValue, modifiedValue);
    }
}

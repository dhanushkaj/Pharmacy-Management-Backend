package com.rdp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/sales")
public class SalesController {
    @PreAuthorize("hasAnyAuthority('admin', 'manager', 'pharmacist')")
    @PostMapping("/process")
    public Map<String, String> processSale(@RequestBody Map<String, Object> payload) {
        // Sales logic here
        return Map.of("status", "Sale processed");
    }
}

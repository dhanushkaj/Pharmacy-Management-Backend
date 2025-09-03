package com.rdp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    @PreAuthorize("hasAnyAuthority('admin', 'manager')")
    @GetMapping("/view")
    public Map<String, String> viewReports() {
        // Reports logic here
        return Map.of("status", "Reports viewed");
    }
}

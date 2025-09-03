package com.rdp.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/grn")
public class GrnController {
    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/approve")
    public Map<String, String> approveGrn(@RequestBody Map<String, Object> payload) {
        // Approve GRN logic here
        return Map.of("status", "GRN approved by admin");
    }
}

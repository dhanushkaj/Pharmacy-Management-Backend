package com.rdp.controller;


import com.rdp.dto.CategoryRequest;
import com.rdp.dto.CategoryResponse;
import com.rdp.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService service;
    public CategoryController(CategoryService service) { this.service = service; }

    @GetMapping
    public List<CategoryResponse> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public CategoryResponse one(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest req) {
        var created = service.create(req);
        return ResponseEntity.created(URI.create("/api/categories/" + created.categoryId())).body(created);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest req) {
        return service.update(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(Map.of(
                "message", "Category Deleted",
                "id", id
        ));
    }
}

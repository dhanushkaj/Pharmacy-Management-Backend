package com.rdp.service;

import com.rdp.dto.CategoryRequest;
import com.rdp.dto.CategoryResponse;
import com.rdp.model.Category;
import com.rdp.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repo;
    public CategoryService(CategoryRepository repo) { this.repo = repo; }

    public List<CategoryResponse> findAll() {
        return repo.findAll().stream()
                .map(c -> new CategoryResponse(c.getCategoryId(), c.getName(), c.getDescription()))
                .toList();
    }

    public CategoryResponse findById(Long id) {
        var c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        return new CategoryResponse(c.getCategoryId(), c.getName(), c.getDescription());
    }

    public CategoryResponse create(CategoryRequest req) {
        var saved = repo.save(Category.builder().name(req.name()).description(req.description()).build());
        return new CategoryResponse(saved.getCategoryId(), saved.getName(), saved.getDescription());
    }

    public CategoryResponse update(Long id, CategoryRequest req) {
        var c = repo.findById(id).orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
        c.setName(req.name());
        c.setDescription(req.description());
        var saved = repo.save(c);
        return new CategoryResponse(saved.getCategoryId(), saved.getName(), saved.getDescription());
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) throw new IllegalArgumentException("Category not found: " + id);
        repo.deleteById(id);
    }
}

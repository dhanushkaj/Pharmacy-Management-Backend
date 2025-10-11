package com.rdp.service;

import com.rdp.dto.CategoryRequest;
import com.rdp.dto.CategoryResponse;
import com.rdp.model.Category;
import com.rdp.repository.CategoryRepository;
import com.rdp.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository repo;
    private final ProductRepository productRepo;

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

        boolean inUse = productRepo.existsByCategoryId(id);
        if (inUse) {
            // friendly message for frontend
            throw new IllegalStateException("Category is still referenced by products");
        }

        repo.deleteById(id);
    }
}

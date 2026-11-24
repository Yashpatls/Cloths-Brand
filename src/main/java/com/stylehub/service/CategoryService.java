package com.stylehub.service;

import com.stylehub.exception.ResourceNotFoundException;
import com.stylehub.model.Category;
import com.stylehub.repo.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepo;

    public CategoryService(CategoryRepository categoryRepo) {
        this.categoryRepo = categoryRepo;
    }

    public List<Category> getAllActive() {
        return categoryRepo.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public List<Category> getAll() {
        return categoryRepo.findAll();
    }

    public Category getById(Long id) {
        return categoryRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
    }

    public Category getBySlug(String slug) {
        return categoryRepo.findBySlugIgnoreCase(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found for slug: " + slug));
    }

    @Transactional
    public Category save(Category category) {
        if (category.getSlug() == null || category.getSlug().isBlank()) {
            category.setSlug(category.getName().trim().toLowerCase().replaceAll("[^a-z0-9]+", "-"));
        }
        return categoryRepo.save(category);
    }

    @Transactional
    public void delete(Long id) {
        Category c = getById(id);
        c.setActive(false);
        categoryRepo.save(c);
    }
}

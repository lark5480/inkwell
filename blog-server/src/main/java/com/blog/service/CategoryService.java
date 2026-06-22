package com.blog.service;

import com.blog.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {

    // Admin methods
    List<CategoryDTO> getAllCategories();

    Long createCategory(CategoryDTO dto);

    void updateCategory(Long id, CategoryDTO dto);

    void deleteCategory(Long id);

    // Web methods
    List<CategoryDTO> getCategories();
}

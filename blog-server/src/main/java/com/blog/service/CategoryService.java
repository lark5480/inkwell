package com.blog.service;

import com.blog.dto.CategoryDTO;

import java.util.List;

/**
 * 分类管理 Service 接口
 */
public interface CategoryService {

    // Admin methods
    /**
     * 获取所有分类
     *
     * @return 所有分类列表
     */
    List<CategoryDTO> getAllCategories();

    /**
     * 创建分类
     *
     * @param dto 分类信息
     * @return 新建分类的 ID
     */
    Long createCategory(CategoryDTO dto);

    /**
     * 更新分类
     *
     * @param id  分类 ID
     * @param dto 新的分类信息
     */
    void updateCategory(Long id, CategoryDTO dto);

    /**
     * 删除分类
     *
     * @param id 分类 ID
     */
    void deleteCategory(Long id);

    // Web methods
    /**
     * 获取前端展示的分类列表（仅包含已发布的分类）
     *
     * @return 前端分类列表
     */
    List<CategoryDTO> getCategories();
}

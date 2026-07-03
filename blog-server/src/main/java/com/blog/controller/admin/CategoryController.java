package com.blog.controller.admin;

import com.blog.common.Result;
import com.blog.dto.CategoryDTO;
import com.blog.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类管理控制器
 */
@RestController("adminCategoryController")
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取所有分类列表
     *
     * @return 分类列表
     */
    @GetMapping
    public Result<List<CategoryDTO>> list() {
        List<CategoryDTO> list = categoryService.getAllCategories();
        return Result.success(list);
    }

    /**
     * 创建新分类
     *
     * @param dto 分类信息
     * @return 新分类 ID
     */
    @PostMapping
    public Result<Long> create(@Valid @RequestBody CategoryDTO dto) {
        Long id = categoryService.createCategory(dto);
        return Result.success(id);
    }

    /**
     * 更新指定分类
     *
     * @param id  分类 ID
     * @param dto 分类信息
     * @return 无
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody CategoryDTO dto) {
        categoryService.updateCategory(id, dto);
        return Result.success();
    }

    /**
     * 删除指定分类
     *
     * @param id 分类 ID
     * @return 无
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return Result.success();
    }
}

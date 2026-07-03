package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.CategoryDTO;
import com.blog.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端分类控制器
 */
@RestController("webCategoryController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取全部分类列表
     *
     * @return 分类列表
     */
    @GetMapping("/categories")
    public Result<List<CategoryDTO>> list() {
        List<CategoryDTO> list = categoryService.getCategories();
        return Result.success(list);
    }
}

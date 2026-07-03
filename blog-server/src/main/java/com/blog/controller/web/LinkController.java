package com.blog.controller.web;

import com.blog.common.Result;
import com.blog.dto.LinkDTO;
import com.blog.service.LinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 友链控制器 - 提供友链列表等前端接口
 */
@RestController("webLinkController")
@RequestMapping("/api/web")
@RequiredArgsConstructor
public class LinkController {

    private final LinkService linkService;

    /**
     * 获取友链列表
     *
     * @return 友链列表，每个友链包含名称、URL、头像等信息
     */
    @GetMapping("/links")
    public Result<List<LinkDTO>> list() {
        List<LinkDTO> list = linkService.getLinks();
        return Result.success(list);
    }
}

package com.blog.service;

import com.blog.dto.LinkDTO;

import java.util.List;

public interface LinkService {

    // Admin methods
    /**
     * 获取所有友链列表（包含未审核的）
     *
     * @return 友链列表
     */
    List<LinkDTO> getAllLinks();

    /**
     * 创建友链
     *
     * @param dto 友链数据
     * @return 创建后的友链 ID
     */
    Long createLink(LinkDTO dto);

    /**
     * 更新友链
     *
     * @param id  友链 ID
     * @param dto 更新后的友链数据
     */
    void updateLink(Long id, LinkDTO dto);

    /**
     * 删除友链
     *
     * @param id 友链 ID
     */
    void deleteLink(Long id);

    // Web methods
    /**
     * 获取已审核通过的友链列表（前端展示用）
     *
     * @return 友链列表
     */
    List<LinkDTO> getLinks();
}

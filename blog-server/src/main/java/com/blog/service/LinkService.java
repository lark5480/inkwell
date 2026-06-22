package com.blog.service;

import com.blog.dto.LinkDTO;

import java.util.List;

public interface LinkService {

    // Admin methods
    List<LinkDTO> getAllLinks();

    Long createLink(LinkDTO dto);

    void updateLink(Long id, LinkDTO dto);

    void deleteLink(Long id);

    // Web methods
    List<LinkDTO> getLinks();
}

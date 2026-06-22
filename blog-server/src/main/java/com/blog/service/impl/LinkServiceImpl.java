package com.blog.service.impl;

import com.blog.common.CacheNames;
import com.blog.dto.LinkDTO;
import com.blog.entity.Link;
import com.blog.exception.BusinessException;
import com.blog.repository.LinkRepository;
import com.blog.service.LinkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LinkServiceImpl implements LinkService {

    private static final Logger log = LoggerFactory.getLogger(LinkServiceImpl.class);

    private final LinkRepository linkRepository;

    public LinkServiceImpl(LinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    /**
     * 后台：获取所有友链
     */
    @Override
    public List<LinkDTO> getAllLinks() {
        log.debug("后台获取所有友链");
        return linkRepository.findByIsDeletedOrderBySort(false).stream()
                .map(this::toLinkDTO)
                .toList();
    }

    /**
     * 后台：创建友链
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.LINK_LIST, allEntries = true)
    public Long createLink(LinkDTO dto) {
        log.info("创建友链 name={} url={}", dto.name(), dto.url());
        Link link = Link.builder()
                .name(dto.name())
                .url(dto.url())
                .avatar(dto.avatar())
                .description(dto.description())
                .sort(dto.sort() != null ? dto.sort() : 0)
                .status(dto.status() != null ? dto.status() : 1)
                .build();
        linkRepository.save(link);
        log.info("友链创建成功 id={}", link.getId());
        return link.getId();
    }

    /**
     * 后台：更新友链
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.LINK_LIST, allEntries = true)
    public void updateLink(Long id, LinkDTO dto) {
        log.info("更新友链 id={}", id);
        Link link = linkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Link not found"));

        if (dto.name() != null) link.setName(dto.name());
        if (dto.url() != null) link.setUrl(dto.url());
        if (dto.avatar() != null) link.setAvatar(dto.avatar());
        if (dto.description() != null) link.setDescription(dto.description());
        if (dto.sort() != null) link.setSort(dto.sort());
        if (dto.status() != null) link.setStatus(dto.status());

        linkRepository.save(link);
        log.info("友链更新完成 id={}", id);
    }

    /**
     * 后台：删除友链
     */
    @Override
    @Transactional
    @CacheEvict(value = CacheNames.LINK_LIST, allEntries = true)
    public void deleteLink(Long id) {
        log.warn("删除友链 id={}", id);
        Link link = linkRepository.findById(id)
                .orElseThrow(() -> new BusinessException(404, "Link not found"));
        linkRepository.delete(link);
    }

    /**
     * C端：获取已启用的友链列表
     */
    @Override
    @Cacheable(value = CacheNames.LINK_LIST)
    public List<LinkDTO> getLinks() {
        log.debug("C端获取友链列表");
        return linkRepository.findByStatus(1).stream()
                .map(this::toLinkDTO)
                .toList();
    }

    private LinkDTO toLinkDTO(Link link) {
        return new LinkDTO(
                link.getId(),
                link.getName(),
                link.getUrl(),
                link.getAvatar(),
                link.getDescription(),
                link.getSort(),
                link.getStatus()
        );
    }
}

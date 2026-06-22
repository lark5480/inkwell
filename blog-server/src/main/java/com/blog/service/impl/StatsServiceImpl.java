package com.blog.service.impl;

import com.blog.dto.StatsOverviewResponse;
import com.blog.repository.ArticleRepository;
import com.blog.repository.CategoryRepository;
import com.blog.repository.CommentRepository;
import com.blog.repository.LinkRepository;
import com.blog.repository.TagRepository;
import com.blog.service.StatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class StatsServiceImpl implements StatsService {

    private static final Logger log = LoggerFactory.getLogger(StatsServiceImpl.class);

    private final ArticleRepository articleRepository;
    private final CommentRepository commentRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final LinkRepository linkRepository;

    public StatsServiceImpl(ArticleRepository articleRepository,
                            CommentRepository commentRepository,
                            CategoryRepository categoryRepository,
                            TagRepository tagRepository,
                            LinkRepository linkRepository) {
        this.articleRepository = articleRepository;
        this.commentRepository = commentRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.linkRepository = linkRepository;
    }

    /**
     * 后台：获取统计概览（文章数/评论数/浏览量等）
     */
    @Override
    public StatsOverviewResponse getOverview() {
        log.debug("后台获取统计概览");
        long articleCount = articleRepository.count();
        long commentCount = commentRepository.count();
        long categoryCount = categoryRepository.count();
        long tagCount = tagRepository.count();
        long linkCount = linkRepository.count();

        /* 用 SUM 聚合查询，避免全量加载到内存 */
        long viewCount = articleRepository.sumViewCount();

        log.debug("统计概览：文章={} 评论={} 浏览={} 分类={} 标签={} 友链={}",
                articleCount, commentCount, viewCount, categoryCount, tagCount, linkCount);
        return new StatsOverviewResponse(
                articleCount,
                commentCount,
                viewCount,
                categoryCount,
                tagCount,
                linkCount
        );
    }
}

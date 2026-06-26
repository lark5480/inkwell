package com.blog.service;

import com.blog.common.CacheNames;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Arrays;

/**
 * Markdown 转 HTML 渲染服务。
 * 基于 flexmark 将 Markdown 内容渲染为 HTML。
 * 渲染结果按 articleId 缓存到 Redis，文章更新时由 ArticleServiceImpl 主动失效。
 */
@Service
public class MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer renderer;

    public MarkdownRenderer() {
        MutableDataSet options = new MutableDataSet();

        // Enable extensions
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                TaskListExtension.create(),
                AutolinkExtension.create()
        ));

        // Table options
        options.set(TablesExtension.WITH_CAPTION, false);
        options.set(TablesExtension.COLUMN_SPANS, false);
        options.set(TablesExtension.APPEND_MISSING_COLUMNS, true);

        // HTML output options
        options.set(HtmlRenderer.SOFT_BREAK, "<br />\n");
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);

        this.parser = Parser.builder(options).build();
        this.renderer = HtmlRenderer.builder(options).build();
    }

    /**
     * 将 Markdown 文本渲染为 HTML。
     *
     * @param markdown Markdown 原文
     * @return 渲染后的 HTML 字符串
     */
    public String render(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        Node document = parser.parse(markdown);
        return renderer.render(document);
    }

    /**
     * 按 articleId 缓存渲染结果。同一篇文章内容不变时直接命中缓存。
     * 文章更新/删除时由 ArticleServiceImpl 调用 evictHtmlCache 失效。
     */
    @Cacheable(value = CacheNames.ARTICLE_HTML, key = "#articleId")
    public String renderForArticle(Long articleId, String markdown) {
        return render(markdown);
    }
}


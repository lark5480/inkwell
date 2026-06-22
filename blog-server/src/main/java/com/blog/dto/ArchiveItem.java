package com.blog.dto;

import java.util.List;

public record ArchiveItem(
        String yearMonth,
        List<ArchiveArticle> articles
) {
}

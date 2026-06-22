package com.blog.dto;

import java.util.List;

public record PageDTO<T>(
        List<T> records,
        long total,
        int page,
        int pageSize
) {
}

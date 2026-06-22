package com.blog.dto;

public record ReportRequest(
        String reason  // "SPAM" / "ABUSE" / "OTHER"
) {
}

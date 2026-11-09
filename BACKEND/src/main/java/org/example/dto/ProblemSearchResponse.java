package org.example.dto;

import java.util.List;

public record ProblemSearchResponse(
        List<ProblemResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

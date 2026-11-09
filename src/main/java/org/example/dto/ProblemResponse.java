package org.example.dto;

import java.util.List;

public record ProblemResponse(
        Long id,
        String title,
        String difficulty,
        Double acceptanceRate,
        String link,
        List<String> topics,
        List<ProblemCompanyDetail> companies
) {}

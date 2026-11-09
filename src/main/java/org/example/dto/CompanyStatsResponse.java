package org.example.dto;

import java.util.List;
import java.util.Map;

public record CompanyStatsResponse(
        String name,
        long totalProblems,
        Map<String, Long> difficultyBreakdown,
        List<TopicCount> topTopics,
        Map<String, Long> recencyBreakdown
) {
    public record TopicCount(String name, long count) {}
}

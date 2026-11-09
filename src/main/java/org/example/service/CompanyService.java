package org.example.service;

import org.example.dto.CompanyResponse;
import org.example.dto.CompanyStatsResponse;
import org.example.dto.CompanyStatsResponse.TopicCount;
import org.example.repository.CompanyProblemRepository;
import org.example.repository.CompanyRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyProblemRepository companyProblemRepository;

    public CompanyService(CompanyRepository companyRepository,
                          CompanyProblemRepository companyProblemRepository) {
        this.companyRepository = companyRepository;
        this.companyProblemRepository = companyProblemRepository;
    }

    public List<CompanyResponse> listCompanies(String search) {
        return companyRepository.findAllWithProblemCount(search);
    }

    public CompanyStatsResponse getStats(String name) {
        companyRepository.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + name));

        long total = companyProblemRepository.countDistinctProblemsByCompanyName(name);

        Map<String, Long> difficultyBreakdown = new LinkedHashMap<>();
        for (Object[] row : companyProblemRepository.difficultyBreakdown(name)) {
            difficultyBreakdown.put((String) row[0], (Long) row[1]);
        }

        List<TopicCount> topTopics = companyProblemRepository.topTopics(name).stream()
                .map(row -> new TopicCount((String) row[0], (Long) row[1]))
                .toList();

        Map<String, Long> recencyBreakdown = new LinkedHashMap<>();
        for (Object[] row : companyProblemRepository.recencyBreakdown(name)) {
            recencyBreakdown.put((String) row[0], (Long) row[1]);
        }

        return new CompanyStatsResponse(name, total, difficultyBreakdown, topTopics, recencyBreakdown);
    }
}

package org.example.service;

import org.example.dto.*;
import org.example.entity.CompanyProblem;
import org.example.entity.Problem;
import org.example.repository.CompanyProblemRepository;
import org.example.repository.ProblemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProblemService {

    private final ProblemRepository problemRepository;
    private final CompanyProblemRepository companyProblemRepository;

    public ProblemService(ProblemRepository problemRepository,
                          CompanyProblemRepository companyProblemRepository) {
        this.problemRepository = problemRepository;
        this.companyProblemRepository = companyProblemRepository;
    }

    public ProblemSearchResponse searchProblems(List<String> companies,
                                                 List<String> difficulties,
                                                 List<String> topics,
                                                 String recency,
                                                 String search,
                                                 Double minFrequency,
                                                 Double maxFrequency,
                                                 String sortBy,
                                                 String sortOrder,
                                                 int page,
                                                 int size) {
        List<String> companyParam = companies == null || companies.isEmpty() ? null : companies;
        List<String> diffParam = difficulties == null || difficulties.isEmpty() ? null : difficulties;
        List<String> topicParam = topics == null || topics.isEmpty() ? null : topics;

        Page<Problem> results;
        if ("frequency".equals(sortBy)) {
            Pageable pageable = PageRequest.of(page, Math.min(size, 100));
            if ("asc".equalsIgnoreCase(sortOrder)) {
                results = problemRepository.searchProblemsSortByFrequencyAsc(
                        companyParam, diffParam, topicParam, recency, search, minFrequency, maxFrequency, pageable);
            } else {
                results = problemRepository.searchProblemsSortByFrequencyDesc(
                        companyParam, diffParam, topicParam, recency, search, minFrequency, maxFrequency, pageable);
            }
        } else {
            Sort sort = buildSort(sortBy, sortOrder);
            Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
            results = problemRepository.searchProblems(
                    companyParam, diffParam, topicParam, recency, search, minFrequency, maxFrequency, pageable);
        }

        List<ProblemResponse> content = results.getContent().stream()
                .map(p -> toProblemResponse(p, companies))
                .toList();

        return new ProblemSearchResponse(content, page, size, results.getTotalElements(), results.getTotalPages());
    }

    public ProblemSearchResponse findOverlap(List<String> companies,
                                              List<String> difficulties,
                                              List<String> topics,
                                              String recency,
                                              int page,
                                              int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));

        Page<Problem> results = problemRepository.findOverlap(
                companies,
                companies.size(),
                difficulties == null || difficulties.isEmpty() ? null : difficulties,
                topics == null || topics.isEmpty() ? null : topics,
                recency,
                pageable
        );

        List<ProblemResponse> content = results.getContent().stream()
                .map(p -> toProblemResponse(p, companies))
                .toList();

        return new ProblemSearchResponse(content, page, size, results.getTotalElements(), results.getTotalPages());
    }

    private ProblemResponse toProblemResponse(Problem problem, List<String> companyFilter) {
        List<String> topicNames = problem.getTopics().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();

        List<CompanyProblem> cps;
        if (companyFilter != null && !companyFilter.isEmpty()) {
            cps = companyProblemRepository.findByProblemIdAndCompanyNameIn(problem.getId(), companyFilter);
        } else {
            cps = companyProblemRepository.findByProblemIdOrderByFrequencyDesc(problem.getId());
            if (cps.size() > 5) {
                cps = cps.subList(0, 5);
            }
        }

        List<ProblemCompanyDetail> companyDetails = cps.stream()
                .map(cp -> new ProblemCompanyDetail(cp.getCompany().getName(), cp.getFrequency(), cp.getRecency()))
                .toList();

        return new ProblemResponse(
                problem.getId(),
                problem.getTitle(),
                problem.getDifficulty(),
                problem.getAcceptanceRate(),
                problem.getLink(),
                topicNames,
                companyDetails
        );
    }

    private Sort buildSort(String sortBy, String sortOrder) {
        String field = switch (sortBy != null ? sortBy : "title") {
            case "acceptance_rate" -> "acceptanceRate";
            case "difficulty" -> "difficulty";
            default -> "title";
        };
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return Sort.by(direction, field);
    }
}

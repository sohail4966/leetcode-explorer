package org.example.controller;

import org.example.dto.ProblemSearchResponse;
import org.example.service.ProblemService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/problems")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public ProblemSearchResponse searchProblems(
            @RequestParam(required = false) List<String> company,
            @RequestParam(required = false) List<String> difficulty,
            @RequestParam(required = false) List<String> topic,
            @RequestParam(required = false) String recency,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Double minFrequency,
            @RequestParam(required = false) Double maxFrequency,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(defaultValue = "asc") String sortOrder,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return problemService.searchProblems(company, difficulty, topic, recency, search,
                minFrequency, maxFrequency, sortBy, sortOrder, page, size);
    }

    @GetMapping("/overlap")
    public ProblemSearchResponse findOverlap(
            @RequestParam List<String> companies,
            @RequestParam(required = false) List<String> difficulty,
            @RequestParam(required = false) List<String> topic,
            @RequestParam(required = false) String recency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return problemService.findOverlap(companies, difficulty, topic, recency, page, size);
    }
}

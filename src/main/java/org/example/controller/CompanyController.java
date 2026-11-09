package org.example.controller;

import org.example.dto.CompanyResponse;
import org.example.dto.CompanyStatsResponse;
import org.example.service.CompanyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    public List<CompanyResponse> listCompanies(@RequestParam(required = false) String search) {
        return companyService.listCompanies(search);
    }

    @GetMapping("/{name}/stats")
    public CompanyStatsResponse getStats(@PathVariable String name) {
        return companyService.getStats(name);
    }
}

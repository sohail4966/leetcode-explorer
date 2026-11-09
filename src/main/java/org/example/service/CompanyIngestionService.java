package org.example.service;

import org.example.entity.*;
import org.example.repository.*;
import org.example.worker.CsvParser;
import org.example.worker.CsvParser.CsvProblemRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Service
public class CompanyIngestionService {

    private static final Logger log = LoggerFactory.getLogger(CompanyIngestionService.class);

    private static final Map<String, String> FILE_TO_RECENCY = Map.of(
            "1. Thirty Days.csv", "THIRTY_DAYS",
            "2. Three Months.csv", "THREE_MONTHS",
            "3. Six Months.csv", "SIX_MONTHS",
            "4. More Than Six Months.csv", "MORE_THAN_SIX_MONTHS",
            "5. All.csv", "ALL"
    );

    private final ProblemRepository problemRepository;
    private final TopicRepository topicRepository;
    private final CompanyRepository companyRepository;
    private final CompanyProblemRepository companyProblemRepository;
    private final CsvParser csvParser;

    public CompanyIngestionService(ProblemRepository problemRepository,
                                   TopicRepository topicRepository,
                                   CompanyRepository companyRepository,
                                   CompanyProblemRepository companyProblemRepository,
                                   CsvParser csvParser) {
        this.problemRepository = problemRepository;
        this.topicRepository = topicRepository;
        this.companyRepository = companyRepository;
        this.companyProblemRepository = companyProblemRepository;
        this.csvParser = csvParser;
    }

    @Transactional
    public void ingestCompany(String companyName, Path companyDir) {
        log.info("Ingesting company: {}", companyName);

        Company company = companyRepository.findByName(companyName)
                .orElseGet(() -> {
                    Company c = new Company();
                    c.setName(companyName);
                    return companyRepository.save(c);
                });

        companyProblemRepository.deleteAllByCompanyId(company.getId());

        try (Stream<Path> csvFiles = Files.list(companyDir)) {
            csvFiles
                    .filter(p -> p.toString().endsWith(".csv"))
                    .forEach(csvFile -> {
                        String fileName = csvFile.getFileName().toString();
                        String recency = FILE_TO_RECENCY.get(fileName);
                        if (recency == null) {
                            log.warn("Unknown CSV file: {}", fileName);
                            return;
                        }
                        ingestCsvFile(company, csvFile, recency);
                    });
        } catch (IOException e) {
            log.error("Failed to list CSV files for company {}", companyName, e);
        }
    }

    private void ingestCsvFile(Company company, Path csvFile, String recency) {
        List<CsvProblemRow> rows = csvParser.parse(csvFile);
        log.debug("Parsed {} rows from {}", rows.size(), csvFile.getFileName());

        for (CsvProblemRow row : rows) {
            Problem problem = problemRepository.findByTitle(row.title)
                    .orElseGet(() -> {
                        Problem p = new Problem();
                        p.setTitle(row.title);
                        p.setDifficulty(row.difficulty);
                        p.setAcceptanceRate(row.acceptanceRate);
                        p.setLink(row.link);
                        return problemRepository.save(p);
                    });

            if (!Objects.equals(problem.getAcceptanceRate(), row.acceptanceRate)) {
                problem.setAcceptanceRate(row.acceptanceRate);
                problemRepository.save(problem);
            }

            Set<Topic> topics = new HashSet<>();
            for (String topicName : row.topics) {
                Topic topic = topicRepository.findByName(topicName)
                        .orElseGet(() -> {
                            Topic t = new Topic();
                            t.setName(topicName);
                            return topicRepository.save(t);
                        });
                topics.add(topic);
            }
            if (!problem.getTopics().equals(topics)) {
                problem.setTopics(topics);
                problemRepository.save(problem);
            }

            CompanyProblem cp = new CompanyProblem();
            cp.setCompany(company);
            cp.setProblem(problem);
            cp.setFrequency(row.frequency);
            cp.setRecency(recency);
            companyProblemRepository.save(cp);
        }
    }
}

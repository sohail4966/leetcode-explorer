package org.example.worker;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class CsvParser {

    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);

    public List<CsvProblemRow> parse(Path csvFile) {
        List<CsvProblemRow> rows = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new FileReader(csvFile.toFile()))) {
            String[] header = reader.readNext();
            if (header == null) return rows;

            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line.length < 6) continue;
                try {
                    CsvProblemRow row = new CsvProblemRow();
                    row.difficulty = line[0].trim();
                    row.title = line[1].trim();
                    row.frequency = parseDouble(line[2].trim());
                    row.acceptanceRate = parseDouble(line[3].trim());
                    row.link = line[4].trim();
                    row.topics = Arrays.stream(line[5].split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .toList();
                    rows.add(row);
                } catch (Exception e) {
                    log.warn("Skipping malformed row in {}: {}", csvFile.getFileName(), e.getMessage());
                }
            }
        } catch (IOException | CsvValidationException e) {
            log.error("Failed to parse CSV file {}: {}", csvFile, e.getMessage());
        }
        return rows;
    }

    private Double parseDouble(String value) {
        if (value == null || value.isEmpty()) return null;
        return Double.parseDouble(value);
    }

    public static class CsvProblemRow {
        public String difficulty;
        public String title;
        public Double frequency;
        public Double acceptanceRate;
        public String link;
        public List<String> topics;
    }
}

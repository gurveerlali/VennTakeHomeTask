package org.venn.takeHomeTest.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.venn.takeHomeTest.transactions.dto.LoadRequest;
import org.venn.takeHomeTest.transactions.dto.LoadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class FileProcessor implements CommandLineRunner {

    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;

    @Value("${app.input-file:input.txt}")
    private String inputFile;

    @Value("${app.output-file:output.txt}")
    private String outputFile;

    public FileProcessor(TransactionService transactionService, ObjectMapper objectMapper) {
        this.transactionService = transactionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        Path inputPath = Path.of(inputFile);
        if (!Files.exists(inputPath)) {
            log.info("No {} found, skipping file processing", inputFile);
            return;
        }

        log.info("Processing {}...", inputFile);
        List<String> lines = Files.readAllLines(inputPath);
        List<String> outputLines = new ArrayList<>();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.isBlank()) {
                continue;
            }

            try {
                LoadRequest request = objectMapper.readValue(line, LoadRequest.class);
                LoadResponse response = transactionService.postLoad(request);

                if (response != null) {
                    outputLines.add(objectMapper.writeValueAsString(response));
                }
            } catch (Exception e) {
                log.warn("Skipping malformed line {}: {} — error: {}", i + 1, line, e.getMessage());
            }
        }

        Path outputPath = Path.of(outputFile);
        Files.write(outputPath, outputLines);
        log.info("Wrote {} results to {}", outputLines.size(), outputFile);
    }
}

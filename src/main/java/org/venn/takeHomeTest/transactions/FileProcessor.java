package org.venn.takeHomeTest.transactions;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.venn.takeHomeTest.transactions.dto.LoadRequest;
import org.venn.takeHomeTest.transactions.dto.LoadResponse;
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

    public FileProcessor(TransactionService transactionService, ObjectMapper objectMapper) {
        this.transactionService = transactionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void run(String... args) throws Exception {
        Path inputPath = Path.of("input.txt");
        if (!Files.exists(inputPath)) {
            log.info("No input.txt found, skipping file processing");
            return;
        }

        log.info("Processing input.txt...");
        List<String> lines = Files.readAllLines(inputPath);
        List<String> outputLines = new ArrayList<>();

        for (String line : lines) {
            if (line.isBlank()) {
                continue;
            }

            LoadRequest request = objectMapper.readValue(line, LoadRequest.class);
            LoadResponse response = transactionService.postLoad(request);

            if (response != null) {
                outputLines.add(objectMapper.writeValueAsString(response));
            }
        }

        Path outputPath = Path.of("output.txt");
        Files.write(outputPath, outputLines);
        log.info("Wrote {} results to output.txt", outputLines.size());
    }
}

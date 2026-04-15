package org.venn.takeHomeTest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        log.info("Start VennTakeHomeTask...");

        try {
            SpringApplication app = new SpringApplication(Main.class);
            app.run(args);
        } catch (Exception e) {
            log.error("Application failed to start", e);
        }
    }
}

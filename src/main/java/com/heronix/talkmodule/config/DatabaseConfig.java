package com.heronix.talkmodule.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.File;

/**
 * Database configuration for local H2 storage.
 * Enables offline-first operation with local data persistence.
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.heronix.talkmodule.repository")
@EnableTransactionManagement
@Slf4j
public class DatabaseConfig {

    @Value("${heronix.database.data-dir:./data}")
    private String dataDirectory;

    @PostConstruct
    public void initialize() {
        File dataDir = new File(dataDirectory);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                log.info("Created local data directory: {}", dataDir.getAbsolutePath());
            }
        }

        log.info("========================================");
        log.info("   Heronix TalkModule Database Init    ");
        log.info("   Data directory: {}                  ", dataDir.getAbsolutePath());
        log.info("========================================");
    }
}

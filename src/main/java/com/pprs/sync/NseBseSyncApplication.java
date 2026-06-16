package com.pprs.sync;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.pprs.sync.service.SyncService;

@SpringBootApplication
@EnableScheduling
public class NseBseSyncApplication {
    public static void main(String[] args) {
        SpringApplication.run(NseBseSyncApplication.class, args);
    }

    @Bean
    CommandLineRunner runOnStartup(SyncService syncService) {
        return args -> {
            // syncService.sync("NSE");
            // syncService.sync("BSE");
            syncService.syncBseDailyPrice();
            // syncService.syncNseDailyPrice();
            // syncService.syncCorporateActions();

        // Or test a specific range
        // syncService.syncCorporateActions(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31));
        };
    }
}

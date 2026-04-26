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
        System.out.println("Application Started");
        SpringApplication.run(NseBseSyncApplication.class, args);
    }

    @Bean
    CommandLineRunner runOnStartup(SyncService syncService) {
        System.out.println("Sync Data now");
        return args -> {
            syncService.sync("NSE");
            syncService.sync("BSE");
        };
    }
}

package com.pprs.sync.scheduler;

import com.pprs.sync.service.SyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SyncScheduler {
    private static final Logger log = LoggerFactory.getLogger(SyncScheduler.class);

    private final SyncService syncService;

    public SyncScheduler(SyncService syncService) {
        this.syncService = syncService;
    }

    // Runs Mon–Fri at 7:30 AM IST (2:00 AM UTC)
    @Scheduled(cron = "0 30 7 * * MON-FRI", zone = "Asia/Kolkata")
    public void runDailySync() {
        log.info("Starting daily sync...");
        // syncService.sync("NSE");
        // syncService.sync("BSE");
        syncService.syncDailyPrice();
        log.info("Daily sync complete.");
    }
}

package com.pprs.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.List;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.io.BufferedWriter;
import com.pprs.sync.model.Security;
import com.pprs.sync.repository.SecurityRepository;
import com.pprs.sync.fetcher.NseFetcher;
import com.pprs.sync.fetcher.BseFetcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final NseFetcher nseFetcher;
    private final BseFetcher bseFetcher;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.archive.dir}")
    private String archiveDir;

    public SyncService(NseFetcher nseFetcher, BseFetcher bseFetcher,
                       JdbcTemplate jdbcTemplate) {
        this.nseFetcher  = nseFetcher;
        this.bseFetcher  = bseFetcher;
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final String UPSERT_SQL = """
        INSERT INTO securities_master
            (isin, symbol, name, exchange, series, face_value, source_updated_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, CURRENT_DATE, NOW())
        ON CONFLICT (isin, exchange) DO UPDATE SET
            symbol            = EXCLUDED.symbol,
            name              = EXCLUDED.name,
            series            = EXCLUDED.series,
            face_value        = EXCLUDED.face_value,
            source_updated_at = EXCLUDED.source_updated_at,
            updated_at        = NOW()
        """;

    public void sync(String exchange) {
        try {
            List<Security> securities = exchange.equals("NSE")
                ? nseFetcher.fetch()
                : bseFetcher.fetch();

            archive(securities, exchange);
            upsertAll(securities);

            log.info("{}: upserted {} records", exchange, securities.size());

        } catch (Exception e) {
            log.error("{} sync failed", exchange, e);
        }
    }

    private void upsertAll(List<Security> securities) {
        List<Object[]> batchArgs = securities.stream()
            .map(s -> new Object[]{
                s.getIsin(),
                s.getSymbol(),
                s.getName(),
                s.getExchange(),
                s.getSeries(),
                s.getFaceValue()
            })
            .toList();

        jdbcTemplate.batchUpdate(UPSERT_SQL, batchArgs);
    }

    private void archive(List<Security> data, String exchange) throws IOException {
        Path dir = Paths.get(archiveDir);
        Files.createDirectories(dir);
        Path file = dir.resolve(exchange + "_" + LocalDate.now() + ".csv");
        try (BufferedWriter writer = Files.newBufferedWriter(file)) {
            writer.write("isin,symbol,name,exchange,series,face_value\n");
            for (Security s : data) {
                writer.write(String.join(",",
                    s.getIsin(), s.getSymbol(), s.getName(),
                    s.getExchange(), s.getSeries(),
                    s.getFaceValue() != null ? s.getFaceValue().toString() : ""
                ) + "\n");
            }
        }
        log.info("Archived {} → {}", exchange, file);
    }
}

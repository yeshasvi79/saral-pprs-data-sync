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

import com.pprs.sync.model.BseDailyPrice;
import com.pprs.sync.model.CorporateAction;
import com.pprs.sync.model.Security;
import com.pprs.sync.repository.SecurityRepository;
import com.pprs.sync.fetcher.NseFetcher;
import com.pprs.sync.fetcher.BseDailyPriceFetcher;
import com.pprs.sync.fetcher.BseFetcher;
import com.pprs.sync.fetcher.CorporateActionFetcher;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class SyncService {
    private static final Logger log = LoggerFactory.getLogger(SyncService.class);

    private final NseFetcher nseFetcher;
    private final BseFetcher bseFetcher;
    private final BseDailyPriceFetcher bseDailyPriceFetcher;
    private final CorporateActionFetcher corporateActionFetcher;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.archive.dir}")
    private String archiveDir;

    public SyncService(NseFetcher nseFetcher, BseFetcher bseFetcher,
        BseDailyPriceFetcher bseDailyPriceFetcher,
        CorporateActionFetcher corporateActionFetcher,
                       JdbcTemplate jdbcTemplate) {
        this.nseFetcher  = nseFetcher;
        this.bseFetcher  = bseFetcher;
        this.bseDailyPriceFetcher = bseDailyPriceFetcher;
        this.corporateActionFetcher = corporateActionFetcher;
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

    private static final String DAILY_PRICE_UPSERT_SQL = """
        INSERT INTO bse_daily_price
            (code, isin, name, open, high, low, close, prev_close,
                volume, turnover, total_trades, trade_date)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (code, trade_date) DO UPDATE SET
            close        = EXCLUDED.close,
            volume       = EXCLUDED.volume,
            turnover     = EXCLUDED.turnover,
            total_trades = EXCLUDED.total_trades
        """;
    private static final String CORPORATE_ACTION_UPSERT_SQL = """
        INSERT INTO corporate_action
            (symbol, isin, company_name, action_type, action_description,
                ex_date, record_date, bc_start_date, bc_end_date,
                nd_start_date, nd_end_date, purpose, exchange)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (isin, action_type, ex_date) DO UPDATE SET
            symbol            = EXCLUDED.symbol,
            company_name      = EXCLUDED.company_name,
            action_description = EXCLUDED.action_description,
            record_date       = EXCLUDED.record_date,
            purpose           = EXCLUDED.purpose,
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

    public void syncDailyPrice() {
        try {
            List<BseDailyPrice> records = bseDailyPriceFetcher.fetchLatest();
            upsertDailyPrice(records);
            log.info("BSE daily price: upserted {} records for {}",
                records.size(), records.get(0).getTradeDate());
        } catch (Exception e) {
            log.error("BSE daily price sync failed", e);
        }
    }

        // Sync latest (previous trading day)
    public void syncCorporateActions() {
        try {
            List<CorporateAction> actions = corporateActionFetcher.fetchLatest();
            upsertCorporateActions(actions);
            log.info("Corporate actions: upserted {} records", actions.size());
        } catch (Exception e) {
            log.error("Corporate actions sync failed", e);
        }
    }

    // Sync for a specific date range
    public void syncCorporateActions(LocalDate fromDate, LocalDate toDate) {
        try {
            List<CorporateAction> actions = corporateActionFetcher.fetch(fromDate, toDate);
            upsertCorporateActions(actions);
            log.info("Corporate actions: upserted {} records from {} to {}",
                actions.size(), fromDate, toDate);
        } catch (Exception e) {
            log.error("Corporate actions sync failed for range {} to {}", fromDate, toDate, e);
        }
    }

    private void upsertCorporateActions(List<CorporateAction> actions) {
        List<Object[]> args = actions.stream()
            .filter(a -> a.getIsin() != null && a.getActionType() != null)
            .map(a -> new Object[]{
                a.getSymbol(), a.getIsin(), a.getCompanyName(),
                a.getActionType(), a.getActionDescription(),
                a.getExDate(), a.getRecordDate(),
                a.getBcStartDate(), a.getBcEndDate(),
                a.getNdStartDate(), a.getNdEndDate(),
                a.getPurpose(), a.getExchange()
            })
            .toList();

        if (args.isEmpty()) {
            log.warn("Corporate actions: nothing to upsert — all records filtered out");
            return;
        }
        jdbcTemplate.batchUpdate(CORPORATE_ACTION_UPSERT_SQL, args);
    }
    
    private void upsertDailyPrice(List<BseDailyPrice> records) {
        List<Object[]> args = records.stream()
            .map(r -> new Object[]{
                r.getCode(), r.getIsin(), r.getName(),
                r.getOpen(), r.getHigh(), r.getLow(),
                r.getClose(), r.getPrevClose(),
                r.getVolume(), r.getTurnover(),
                r.getTotalTrades(), r.getTradeDate()
            })
            .toList();
        jdbcTemplate.batchUpdate(DAILY_PRICE_UPSERT_SQL, args);
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

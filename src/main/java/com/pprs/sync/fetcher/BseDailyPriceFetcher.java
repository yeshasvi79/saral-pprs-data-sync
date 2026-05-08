package com.pprs.sync.fetcher;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pprs.sync.model.BseDailyPrice;

@Component
public class BseDailyPriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(BseDailyPriceFetcher.class);

    private static final String BSE_BHAVCOPY_URL =
        "https://www.bseindia.com/download/BhavCopy/Equity/EQ%s%s%s_CSV.zip";

    // private static final String YAHOO_CSV_URL =
    //     "https://query1.finance.yahoo.com/v7/finance/download/%s.BO" +
    //     "?period1=%d&period2=%d&interval=1d&events=history&includeAdjustedClose=true";

    private static final String YAHOO_JSON_URL =
    "https://query1.finance.yahoo.com/v8/finance/chart/%s.BO" +
    "?interval=1d&range=1d&includePrePost=false";

    private static final String FETCH_BSE_CODES_SQL =
        "SELECT DISTINCT symbol FROM securities_master WHERE exchange = 'BSE' AND symbol IS NOT NULL";

    private final RestTemplate restTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public BseDailyPriceFetcher(RestTemplate restTemplate, JdbcTemplate jdbcTemplate,
        ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<BseDailyPrice> fetch(LocalDate date) throws Exception {
        // Try BSE ZIP first
        try {
            List<BseDailyPrice> records = fetchFromBse(date);
            if (!records.isEmpty()) {
                log.info("BSE ZIP: fetched {} records for {}", records.size(), date);
                return records;
            }
            log.warn("BSE ZIP returned empty for {} — falling back to Yahoo Finance", date);
        } catch (Exception e) {
            log.warn("BSE ZIP fetch failed for {} — falling back to Yahoo Finance. Reason: {}",
                date, e.getMessage());
        }

        // Fallback to Yahoo Finance
        return fetchFromYahoo(date);
    }

    public List<BseDailyPrice> fetchLatest() throws Exception {
        return fetch(previousTradingDay());
    }

    // ─── BSE ZIP ────────────────────────────────────────────────────────────────

    private List<BseDailyPrice> fetchFromBse(LocalDate date) throws Exception {
        String url = buildBseUrl(date);
        log.info("Fetching BSE daily price ZIP from: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        headers.set("Referer", "https://www.bseindia.com");
        headers.set("Accept",  "application/zip, */*");

        ResponseEntity<byte[]> response = restTemplate.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(headers),
            byte[].class
        );

        if (response.getBody() == null || response.getBody().length == 0) {
            return Collections.emptyList();
        }

        return parseZip(response.getBody(), date);
    }

    private List<BseDailyPrice> parseZip(byte[] zipBytes, LocalDate date) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".CSV") || entry.getName().endsWith(".csv")) {
                    log.info("Parsing ZIP entry: {}", entry.getName());
                    return parseBseCsv(zis, date);
                }
            }
        }
        return Collections.emptyList();
    }

    private List<BseDailyPrice> parseBseCsv(InputStream is, LocalDate date) throws Exception {
        List<BseDailyPrice> records = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(is, StandardCharsets.UTF_8)
        );

        String headerLine = reader.readLine();
        log.debug("BSE ZIP CSV header: {}", headerLine);

        // BSE Bhavcopy columns:
        // CODE, NAME, OPEN, HIGH, LOW, CLOSE, PREVCLOSE,
        // TOTTRDQTY, TOTTRDVAL, TOTALTRADES, ISIN_CODE
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 11) continue;

            try {
                records.add(new BseDailyPrice(
                    clean(cols[0]),           // CODE
                    clean(cols[10]),          // ISIN_CODE
                    clean(cols[1]),           // NAME
                    parseBigDecimal(cols[2]), // OPEN
                    parseBigDecimal(cols[3]), // HIGH
                    parseBigDecimal(cols[4]), // LOW
                    parseBigDecimal(cols[5]), // CLOSE
                    parseBigDecimal(cols[6]), // PREVCLOSE
                    parseLong(cols[7]),       // TOTTRDQTY
                    parseBigDecimal(cols[8]), // TOTTRDVAL
                    parseLong(cols[9]),       // TOTALTRADES
                    date
                ));
            } catch (Exception e) {
                log.warn("Skipping malformed BSE row: {} — {}", line, e.getMessage());
            }
        }
        return records;
    }

    // ─── Yahoo Finance Fallback ──────────────────────────────────────────────────

    // 
    
    private List<BseDailyPrice> fetchFromYahoo(LocalDate date) {
        List<String> bseCodes = jdbcTemplate.queryForList(FETCH_BSE_CODES_SQL, String.class);
        log.info("Yahoo Finance fallback: fetching {} BSE symbols for {}", bseCodes.size(), date);
    
        List<BseDailyPrice> records = new ArrayList<>();
        int successCount            = 0;
        int failCount               = 0;
    
        for (String code : bseCodes) {
            try {
                BseDailyPrice price = fetchYahooSymbol(code, date, 0, 0); // periods unused
                if (price != null) {
                    records.add(price);
                    successCount++;
                }
                Thread.sleep(200); // stay under Yahoo rate limit
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                log.error("Yahoo Finance fetch interrupted");
                break;
            } catch (Exception e) {
                failCount++;
                log.warn("Yahoo Finance: failed for {} — {}", code, e.getMessage());
            }
        }
    
        log.info("Yahoo Finance fallback complete — success: {}, failed: {}, date: {}",
            successCount, failCount, date);
        return records;
    }

    // private BseDailyPrice fetchYahooSymbol(String code, LocalDate date,
    //                                        long periodStart, long periodEnd) throws Exception {
    //     String url = String.format(YAHOO_CSV_URL, code, periodStart, periodEnd);

    //     HttpHeaders headers = new HttpHeaders();
    //     headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
    //     headers.set("Accept",    "text/csv, text/plain, */*");

    //     ResponseEntity<String> response = restTemplate.exchange(
    //         url, HttpMethod.GET,
    //         new HttpEntity<>(headers),
    //         String.class
    //     );

    //     if (response.getBody() == null || response.getBody().isBlank()) {
    //         return null;
    //     }

    //     return parseYahooCsv(response.getBody(), code, date);
    // }

    // private BseDailyPrice parseYahooCsv(String csv, String code, LocalDate date) {
    //     // Yahoo CSV format:
    //     // Date,Open,High,Low,Close,Adj Close,Volume
    //     BufferedReader reader = new BufferedReader(new StringReader(csv));
    //     try {
    //         reader.readLine(); // skip header
    //         String line = reader.readLine();
    //         if (line == null || line.isBlank()) return null;

    //         String[] cols = line.split(",", -1);
    //         if (cols.length < 7) return null;

    //         // Yahoo returns "null" strings when market was closed
    //         if (cols[1].equalsIgnoreCase("null")) return null;

    //         return new BseDailyPrice(
    //             code,
    //             null,                     // ISIN not available from Yahoo
    //             null,                     // name not available from Yahoo
    //             parseBigDecimal(cols[1]), // Open
    //             parseBigDecimal(cols[2]), // High
    //             parseBigDecimal(cols[3]), // Low
    //             parseBigDecimal(cols[4]), // Close
    //             null,                     // prevClose not in Yahoo CSV
    //             parseLong(cols[6]),       // Volume
    //             null,                     // turnover not in Yahoo CSV
    //             null,                     // totalTrades not in Yahoo CSV
    //             date
    //         );
    //     } catch (Exception e) {
    //         log.warn("Could not parse Yahoo CSV for {}: {}", code, e.getMessage());
    //         return null;
    //     }
    // }

    // // ─── Helpers ────────────────────────────────────────────────────────────────

    private String buildBseUrl(LocalDate date) {
        String dd = String.format("%02d", date.getDayOfMonth());
        String mm = String.format("%02d", date.getMonthValue());
        String yy = String.format("%02d", date.getYear() % 100);
        return String.format(BSE_BHAVCOPY_URL, dd, mm, yy);
    }

    private LocalDate previousTradingDay() {
        LocalDate date = LocalDate.now().minusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
               date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private String clean(String val) {
        return val == null ? null : val.trim();
    }

    private BigDecimal parseBigDecimal(String val) {
        try { return new BigDecimal(val.trim()); }
        catch (Exception e) { return null; }
    }

    private Long parseLong(String val) {
        try { return Long.parseLong(val.trim()); }
        catch (Exception e) { return null; }
    }

    private BseDailyPrice fetchYahooSymbol(String code, LocalDate date,
            long periodStart, long periodEnd) throws Exception {
    String url = String.format(YAHOO_JSON_URL, code);
    log.warn("Yahoo JSON URL: {}", url);

    HttpHeaders headers = new HttpHeaders();
    headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
    headers.set("Accept",     "application/json");

    ResponseEntity<String> response = restTemplate.exchange(
    url, HttpMethod.GET,
    new HttpEntity<>(headers),
    String.class
    );

    if (response.getBody() == null || response.getBody().isBlank()) {
    return null;
    }

    return parseYahooJson(response.getBody(), code, date);
    }

    private BseDailyPrice parseYahooJson(String json, String code, LocalDate date) throws Exception {
    JsonNode root = objectMapper.readTree(json);

    // Response path: chart.result[0]
    JsonNode result = root.path("chart").path("result");
    if (result.isMissingNode() || !result.isArray() || result.isEmpty()) {
    log.warn("Yahoo JSON: no result for {}", code);
    return null;
    }

    JsonNode data   = result.get(0);
    JsonNode meta   = data.path("meta");
    JsonNode quote  = data.path("indicators").path("quote").get(0);

    if (quote == null) {
    log.warn("Yahoo JSON: no quote data for {}", code);
    return null;
    }

    // Yahoo returns arrays — take index 0 (single day range=1d)
    BigDecimal open  = firstDecimal(quote.path("open"));
    BigDecimal high  = firstDecimal(quote.path("high"));
    BigDecimal low   = firstDecimal(quote.path("low"));
    BigDecimal close = firstDecimal(quote.path("close"));
    Long volume      = firstLong(quote.path("volume"));

    // prevClose lives in meta
    BigDecimal prevClose = meta.hasNonNull("chartPreviousClose")
    ? meta.get("chartPreviousClose").decimalValue()
    : null;

    if (close == null) {
    log.warn("Yahoo JSON: null close price for {} on {}", code, date);
    return null;
    }

    return new BseDailyPrice(
    code,
    null,      // ISIN — not in Yahoo response, enrich via securities_master join
    null,      // name — not in Yahoo response
    open,
    high,
    low,
    close,
    prevClose,
    volume,
    null,      // turnover — not in Yahoo response
    null,      // totalTrades — not in Yahoo response
    date
    );
    }

    // ─── JSON array helpers ──────────────────────────────────────────────────────

    private BigDecimal firstDecimal(JsonNode arrayNode) {
    if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return null;
    JsonNode first = arrayNode.get(0);
    if (first == null || first.isNull()) return null;
    try { return first.decimalValue(); }
    catch (Exception e) { return null; }
    }

    private Long firstLong(JsonNode arrayNode) {
    if (arrayNode == null || !arrayNode.isArray() || arrayNode.isEmpty()) return null;
    JsonNode first = arrayNode.get(0);
    if (first == null || first.isNull()) return null;
    try { return first.longValue(); }
    catch (Exception e) { return null; }
    }
}

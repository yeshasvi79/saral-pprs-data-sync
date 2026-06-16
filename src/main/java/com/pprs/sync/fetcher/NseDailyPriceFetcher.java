package com.pprs.sync.fetcher;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pprs.sync.model.DailyPrice;

@Component
public class NseDailyPriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(NseDailyPriceFetcher.class);

    private static final String NSE_BHAVCOPY_URL =
        "https://nsearchives.nseindia.com/content/cm/BhavCopy_NSE_CM_0_0_0_%s_F_0000.csv.zip";

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("yyyyMMdd");

    private final RestTemplate restTemplate;

    public NseDailyPriceFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<DailyPrice> fetchLatest() throws Exception {
        return fetch(previousTradingDay());
    }

    public List<DailyPrice> fetch(LocalDate date) throws Exception {
        String url = String.format(NSE_BHAVCOPY_URL, date.format(DATE_FORMATTER));
        log.info("Fetching NSE daily price from: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Referer",    "https://www.nseindia.com");
        headers.set("Accept",     "application/zip, */*");

        ResponseEntity<byte[]> response = restTemplate.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(headers),
            byte[].class
        );

        if (response.getBody() == null || response.getBody().length == 0) {
            log.warn("NSE daily price: empty response for {}", date);
            return Collections.emptyList();
        }

        List<DailyPrice> records = parseZip(response.getBody(), date);
        log.info("NSE daily price: parsed {} records for {}", records.size(), date);
        return records;
    }

    // ─── ZIP Parsing ─────────────────────────────────────────────────────────────

    private List<DailyPrice> parseZip(byte[] zipBytes, LocalDate date) throws Exception {
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".csv")) {
                    log.info("Parsing ZIP entry: {}", entry.getName());
                    return parseCsv(zis, date);
                }
            }
        }
        log.warn("NSE daily price: no CSV found in ZIP for {}", date);
        return Collections.emptyList();
    }

    private List<DailyPrice> parseCsv(InputStream is, LocalDate date) throws Exception {
        List<DailyPrice> records = new ArrayList<>();

        BufferedReader reader = new BufferedReader(
            new InputStreamReader(is, StandardCharsets.UTF_8)
        );

        String headerLine = reader.readLine();
        if (headerLine == null) {
            log.warn("NSE daily price: empty CSV for {}", date);
            return Collections.emptyList();
        }
        log.debug("NSE daily price CSV header: {}", headerLine);

        String[] headers = headerLine.split(",", -1);
        int idxIsin      = findColumn(headers, "ISIN");
        int idxSymbol    = findColumn(headers, "TckrSymb");
        int idxName      = findColumn(headers, "FinInstrmNm");
        int idxOpen      = findColumn(headers, "OpnPric");
        int idxHigh      = findColumn(headers, "HghPric");
        int idxLow       = findColumn(headers, "LwPric");
        int idxClose     = findColumn(headers, "ClsPric");
        int idxPrevClose = findColumn(headers, "PrvsClsgPric");
        int idxVolume    = findColumn(headers, "TtlTradgVol");
        int idxTurnover  = findColumn(headers, "TtlTrfVal");
        int idxTrades    = findColumn(headers, "TtlNbOfTxsExctd");

        log.debug("NSE column indexes — isin:{} symbol:{} open:{} high:{} low:{} close:{}",
            idxIsin, idxSymbol, idxOpen, idxHigh, idxLow, idxClose);

        String line;
        int lineNum = 1;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.isBlank()) continue;

            String[] cols = line.split(",", -1);
            try {
                String isin = safeGet(cols, idxIsin);
                if (isin == null || isin.isBlank()) {
                    log.warn("Skipping row {} — missing ISIN", lineNum);
                    continue;
                }

                records.add(new DailyPrice(
                    safeGet(cols, idxSymbol),
                    isin,
                    safeGet(cols, idxName),
                    "NSE",
                    parseBigDecimal(safeGet(cols, idxOpen)),
                    parseBigDecimal(safeGet(cols, idxHigh)),
                    parseBigDecimal(safeGet(cols, idxLow)),
                    parseBigDecimal(safeGet(cols, idxClose)),
                    parseBigDecimal(safeGet(cols, idxPrevClose)),
                    parseLong(safeGet(cols, idxVolume)),
                    parseBigDecimal(safeGet(cols, idxTurnover)),
                    parseLong(safeGet(cols, idxTrades)),
                    date
                ));
            } catch (Exception e) {
                log.warn("Skipping malformed row {}: {} — {}", lineNum, line, e.getMessage());
            }
        }
        return records;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(name)) return i;
        }
        log.warn("NSE daily price: column '{}' not found in header", name);
        return -1;
    }

    private String safeGet(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        String val = cols[idx].trim();
        return val.isEmpty() ? null : val;
    }

    private LocalDate previousTradingDay() {
        LocalDate date = LocalDate.now().minusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
               date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private BigDecimal parseBigDecimal(String val) {
        try { return new BigDecimal(val.trim()); }
        catch (Exception e) { return null; }
    }

    private Long parseLong(String val) {
        try { return Long.parseLong(val.trim()); }
        catch (Exception e) { return null; }
    }
}

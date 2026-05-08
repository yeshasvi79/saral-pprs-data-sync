package com.pprs.sync.fetcher;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.pprs.sync.model.BseDailyPrice;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class xxBseDailyPriceFetcher {

    private static final Logger log = LoggerFactory.getLogger(BseDailyPriceFetcher.class);

    private static final String BSE_BHAVCOPY_URL =
        "https://www.bseindia.com/download/BhavCopy/Equity/EQ%s%s%s_CSV.zip";

    private final RestTemplate restTemplate;

    public xxBseDailyPriceFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<BseDailyPrice> fetch(LocalDate date) throws Exception {
        String url = buildUrl(date);
        log.info("Fetching BSE daily price from: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36");
        headers.set("Referer", "https://www.bseindia.com");
        headers.set("Accept", "application/zip, */*");

        ResponseEntity<byte[]> response = restTemplate.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(headers),
            byte[].class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("BSE daily price response body is null for date: " + date);
        }

        List<BseDailyPrice> records = parseZip(response.getBody(), date);
        log.info("BSE daily price: parsed {} records for {}", records.size(), date);
        return records;
    }

    public List<BseDailyPrice> fetchLatest() throws Exception {
        return fetch(previousTradingDay());
    }

    private List<BseDailyPrice> parseZip(byte[] zipBytes, LocalDate date) throws Exception {
        List<BseDailyPrice> records = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith(".CSV") || entry.getName().endsWith(".csv")) {
                    log.info("Parsing ZIP entry: {}", entry.getName());
                    records = parseCsv(zis, date);
                    break;
                }
            }
        }

        if (records.isEmpty()) {
            throw new IllegalStateException("No CSV found inside ZIP for date: " + date);
        }
        return records;
    }

    private List<BseDailyPrice> parseCsv(InputStream is, LocalDate date) throws Exception {
        List<BseDailyPrice> records = new ArrayList<>();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(is, StandardCharsets.UTF_8)
        );

        String headerLine = reader.readLine();
        log.debug("BSE daily price CSV header: {}", headerLine);

        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank()) continue;
            String[] cols = line.split(",", -1);
            if (cols.length < 11) continue;

            try {
                records.add(new BseDailyPrice(
                    clean(cols[0]),          // CODE
                    clean(cols[10]),         // ISIN_CODE
                    clean(cols[1]),          // NAME
                    parseBigDecimal(cols[2]),// OPEN
                    parseBigDecimal(cols[3]),// HIGH
                    parseBigDecimal(cols[4]),// LOW
                    parseBigDecimal(cols[5]),// CLOSE
                    parseBigDecimal(cols[6]),// PREVCLOSE
                    parseLong(cols[7]),      // TOTTRDQTY
                    parseBigDecimal(cols[8]),// TOTTRDVAL
                    parseLong(cols[9]),      // TOTALTRADES
                    date
                ));
            } catch (Exception e) {
                log.warn("Skipping malformed row: {} — {}", line, e.getMessage());
            }
        }
        return records;
    }

    private String buildUrl(LocalDate date) {
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
        date = date.minusDays(2);
        return date;
    }

    private String clean(String val)          { return val == null ? null : val.trim(); }
    private BigDecimal parseBigDecimal(String val) {
        try { return new BigDecimal(val.trim()); } catch (Exception e) { return null; }
    }
    private Long parseLong(String val) {
        try { return Long.parseLong(val.trim()); } catch (Exception e) { return null; }
    }
}

package com.pprs.sync.fetcher;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.opencsv.CSVReader;
import com.pprs.sync.model.Security;

@Component
public class BseFetcher {

    private static final Logger log = LoggerFactory.getLogger(BseFetcher.class);

    private static final String BSE_URL =
        "https://www.bseindia.com/downloads1/List_of_companies.csv";

    private final RestTemplate restTemplate;

    public BseFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Security> fetch() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Referer", "https://www.bseindia.com");
        headers.set("Accept", "text/csv, text/plain, */*");
        headers.set("Accept-Language", "en-US,en;q=0.9");

        ResponseEntity<String> response = restTemplate.exchange(
            BSE_URL, HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("BSE List_of_companies.csv response is null");
        }

        log.debug("BSE raw response (first 300 chars): {}",
            response.getBody().substring(0, Math.min(300, response.getBody().length())));

       //  List<Security> securities = parseCsv(response.getBody());

       List<Security> securities = new ArrayList<>();
       try (CSVReader reader = new CSVReader(new StringReader(response.getBody()))) {
           String[] headers2 = reader.readNext(); // skip header
           String[] line;
           while ((line = reader.readNext()) != null) {
               if (line[0].isBlank()) continue;
               securities.add(Security.builder()
                   .symbol(line[1].trim())
                   .name(line[2].trim())
                   //.series(line[2].trim())
                   .isin(line[3].trim())
                   //.faceValue(parseBigDecimal(line[7]))
                   .exchange("BSE")
                   .build());
           }
       }

        log.info("BSE: fetched {} records", securities.size());
        return securities;
    }

    private List<Security> parseCsv(String csvBody) throws Exception {
        List<Security> securities = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new StringReader(csvBody));

        String headerLine = reader.readLine();
        log.debug("BSE CSV header: {}", headerLine);

        // Parse header to find column indexes dynamically
        // — BSE occasionally reorders columns across file versions
        String[] headers = headerLine.split(",", -1);
        int idxIsin      = findColumn(headers, "ISIN", "ISIN");
        int idxSymbol    = findColumn(headers, "Scrip code", "SCRIP_CODE");
        int idxName      = findColumn(headers, "Security Name", "COMPANY_NAME");
        int idxSeries    = findColumn(headers, "GROUP", "SERIES");
        int idxFaceValue = findColumn(headers, "FACE_VALUE", "FACEVALUE");

        log.info("BSE column indexes — isin:{} symbol:{} name:{} series:{} faceValue:{}",
            idxIsin, idxSymbol, idxName, idxSeries, idxFaceValue);

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

                securities.add(Security.builder()
                    .isin(isin)
                    .symbol(safeGet(cols, idxSymbol))
                    .name(safeGet(cols, idxName))
                    .exchange("BSE")
                    .series(safeGet(cols, idxSeries))
                    .faceValue(parseBigDecimal(safeGet(cols, idxFaceValue)))
                    .build());

            } catch (Exception e) {
                log.warn("Skipping malformed row {}: {} — {}", lineNum, line, e.getMessage());
            }
        }
        return securities;
    }

    // Finds the first matching column name (case-insensitive) from a list of candidates
    private int findColumn(String[] headers, String... candidates) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toUpperCase();
            for (String candidate : candidates) {
                if (h.equals(candidate.toUpperCase())) return i;
            }
        }
        log.warn("BSE column not found for candidates: {}", (Object) candidates);
        return -1;
    }

    private String safeGet(String[] cols, int idx) {
        if (idx < 0 || idx >= cols.length) return null;
        String val = cols[idx].trim();
        return val.isEmpty() ? null : val;
    }

    private BigDecimal parseBigDecimal(String val) {
        try { return new BigDecimal(val.trim()); }
        catch (Exception e) { return null; }
    }
}

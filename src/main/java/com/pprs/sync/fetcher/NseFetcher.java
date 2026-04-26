package com.pprs.sync.fetcher;

import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.opencsv.CSVReader;
import com.pprs.sync.model.Security;
import java.math.BigDecimal;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class NseFetcher {
    private static final Logger log = LoggerFactory.getLogger(NseFetcher.class);

    private static final String NSE_URL =
        "https://nsearchives.nseindia.com/content/equities/EQUITY_L.csv";

    private final RestTemplate restTemplate;

    public NseFetcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Security> fetch() throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Referer", "https://www.nseindia.com");

        ResponseEntity<String> response = restTemplate.exchange(
            NSE_URL, HttpMethod.GET, new HttpEntity<>(headers), String.class
        );

        List<Security> securities = new ArrayList<>();
        try (CSVReader reader = new CSVReader(new StringReader(response.getBody()))) {
            String[] headers2 = reader.readNext(); // skip header
            String[] line;
            while ((line = reader.readNext()) != null) {
                if (line[0].isBlank()) continue;
                securities.add(Security.builder()
                    .symbol(line[0].trim())
                    .name(line[1].trim())
                    .series(line[2].trim())
                    .isin(line[6].trim())
                    .faceValue(parseBigDecimal(line[7]))
                    .exchange("NSE")
                    .build());
            }
        }
        log.info("NSE: fetched {} records", securities.size());
        return securities;
    }

    private BigDecimal parseBigDecimal(String val) {
        try { return new BigDecimal(val.trim()); }
        catch (Exception e) { return null; }
    }
}

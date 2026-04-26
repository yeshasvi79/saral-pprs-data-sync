package com.pprs.sync.fetcher;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pprs.sync.model.Security;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class BseFetcher {
    private static final Logger log = LoggerFactory.getLogger(BseFetcher.class);

    private static final String BSE_URL =
        "https://www.bseindia.com/corporates/List_Scrips.aspx";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BseFetcher(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Security> fetch() throws Exception {
        // String response = restTemplate.getForObject(BSE_URL, String.class);
        // JsonNode root = objectMapper.readTree(response);
        // JsonNode table = root.get("Table");

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Referer", "https://www.bseindia.com");
        headers.set("Accept", "application/json, text/plain, */*");
        headers.set("Accept-Language", "en-US,en;q=0.9");
        headers.set("Origin", "https://www.bseindia.com");

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            BSE_URL, HttpMethod.GET, entity, String.class
        );

        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode table = root.get("Table");

        if (table == null || !table.isArray()) {
            throw new Exception("BSE returned empty or invalid Table payload");
        }

        List<Security> securities = new ArrayList<>();
        for (JsonNode node : table) {
            securities.add(Security.builder()
                .isin(node.get("ISIN_NO").asText().trim())
                .symbol(node.get("SCRIP_CD").asText().trim())
                .name(node.get("SCRIP_NAME").asText().trim())
                .exchange("BSE")
                .series(node.get("GROUP").asText().trim())
                .faceValue(parseBigDecimal(node.get("FACE_VALUE").asText()))
                .build());
        }
        log.info("BSE: fetched {} records", securities.size());
        return securities;
    }

    private BigDecimal parseBigDecimal(String val) {
        try { return new BigDecimal(val.trim()); }
        catch (Exception e) { return null; }
    }
}

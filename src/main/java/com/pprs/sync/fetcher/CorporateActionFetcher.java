package com.pprs.sync.fetcher;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pprs.sync.model.CorporateAction;

@Component
public class CorporateActionFetcher {

    private static final Logger log = LoggerFactory.getLogger(CorporateActionFetcher.class);

    private static final String NSE_CA_URL =
        "https://www.nseindia.com/api/corporate-announcements"
        + "?index=equities&from_date=%s&to_date=%s";

    private static final String NSE_DATE_FORMAT  = "dd-MM-yyyy";
    private static final DateTimeFormatter NSE_FORMATTER =
        DateTimeFormatter.ofPattern(NSE_DATE_FORMAT);

    // NSE response date fields use this format
    private static final DateTimeFormatter RESPONSE_FORMATTER =
        DateTimeFormatter.ofPattern("dd-MMM-yyyy");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public CorporateActionFetcher(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    // Fetch previous trading day by default
    public List<CorporateAction> fetchLatest() throws Exception {
        LocalDate prev = previousTradingDay();
        return fetch(prev, prev);
    }

    // Fetch for a configurable date range
    public List<CorporateAction> fetch(LocalDate fromDate, LocalDate toDate) throws Exception {
        String url = String.format(NSE_CA_URL,
            fromDate.format(NSE_FORMATTER),
            toDate.format(NSE_FORMATTER)
        );
        log.info("Fetching NSE corporate actions: {}", url);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Referer",         "https://www.nseindia.com");
        headers.set("Accept",          "application/json, text/plain, */*");
        headers.set("Accept-Language", "en-US,en;q=0.9");
        headers.set("X-Requested-With", "XMLHttpRequest");

        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET,
            new HttpEntity<>(headers),
            String.class
        );

        if (response.getBody() == null) {
            throw new IllegalStateException("NSE corporate actions response is null for range: "
                + fromDate + " to " + toDate);
        }

        List<CorporateAction> actions = parseResponse(response.getBody());
        log.info("NSE corporate actions: parsed {} records from {} to {}",
            actions.size(), fromDate, toDate);
        return actions;
    }

    private List<CorporateAction> parseResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);

        // NSE returns either a root array or a wrapper object
        JsonNode dataNode = root.isArray() ? root : root.get("data");
        if (dataNode == null || !dataNode.isArray()) {
            log.warn("NSE corporate actions: unexpected response structure");
            return Collections.emptyList();
        }

        List<CorporateAction> actions = new ArrayList<>();
        for (JsonNode node : dataNode) {
            try {
                actions.add(new CorporateAction(
                    textOrNull(node, "symbol"),
                    textOrNull(node, "sm_isin"),
                    textOrNull(node, "sm_name"),
                    textOrNull(node, "desc"),
                    textOrNull(node, "attchmntText"),
                    parseDate(textOrNull(node, "an_dt")),
                    parseDate(textOrNull(node, "sort_date")),
                    null, null, null, null,
                    textOrNull(node, "attchmntFile"),
                    "NSE"
                ));
                // parseDate(textOrNull(node, "bcStartDate")),
                //     parseDate(textOrNull(node, "bcEndDate")),
                //     parseDate(textOrNull(node, "ndStartDate")),
                //     parseDate(textOrNull(node, "ndEndDate")),
            } catch (Exception e) {
                log.warn("Skipping malformed corporate action node: {} — {}", node, e.getMessage());
            }
        }
        return actions;
    }

    private LocalDate previousTradingDay() {
        LocalDate date = LocalDate.now().minusDays(1);
        while (date.getDayOfWeek() == DayOfWeek.SATURDAY ||
               date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.minusDays(1);
        }
        return date;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) return null;
        String text = value.asText().trim();
        return text.isEmpty() || text.equalsIgnoreCase("null") ? null : text;
    }

    private LocalDate parseDate(String val) {
        if (val == null || val.isBlank()) return null;
        try {
            return LocalDate.parse(val, RESPONSE_FORMATTER);
        } catch (Exception e) {
            log.warn("Could not parse date: '{}'", val);
            return null;
        }
    }
}

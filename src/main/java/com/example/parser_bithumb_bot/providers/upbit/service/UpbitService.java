package com.example.parser_bithumb_bot.providers.upbit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

@Service
public class UpbitService {

    private static final String API_URL = "https://api.upbit.com/v1/market/all";

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public Set<String> fetchPairs() throws Exception {
        String resp = rest.getForObject(API_URL, String.class);
        JsonNode data = mapper.readTree(resp);

        Set<String> pairs = new HashSet<>();

        for (JsonNode item : data) {
            if (item.has("market")) {
                String market = item.get("market").asText();
                pairs.add(market.split("-")[1]);
            }
        }

        return pairs;
    }
}

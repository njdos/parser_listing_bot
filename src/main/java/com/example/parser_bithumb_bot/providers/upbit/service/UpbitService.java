package com.example.parser_bithumb_bot.providers.upbit.service;

import com.example.parser_bithumb_bot.app.mapper.Mapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class UpbitService {

    private static final String API_URL = "https://api.upbit.com/v1/market/all";

    @Autowired
    private Mapper mapper;

    public Set<String> fetchPairs() throws Exception {
        String resp = mapper.getRestTemplate().getForObject(API_URL, String.class);
        JsonNode data = mapper.getObjectMapper().readTree(resp);

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

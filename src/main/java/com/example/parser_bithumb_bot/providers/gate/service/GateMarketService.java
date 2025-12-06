package com.example.parser_bithumb_bot.providers.gate.service;

import com.example.parser_bithumb_bot.app.mapper.Mapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
public class GateMarketService {

    private static final String PUBLIC_PRICE_URL = "https://api.gateio.ws/api/v4/futures/usdt/tickers";

    @Autowired
    private Mapper mapper;

    public Map<String, Double> getPrice(Set<String> tokens) {
        Map<String, Double> entryPrices = new HashMap<>();
        try {
            String resp = mapper.getRestTemplate().getForObject(PUBLIC_PRICE_URL, String.class);
            JsonNode root = mapper.getObjectMapper().readTree(resp);

            for (String token : tokens) {
                String contract = token.trim().toUpperCase() + "_USDT";
                for (JsonNode node : root) {
                    if (node.get("contract").asText().equalsIgnoreCase(contract)) {
                        entryPrices.put(token, node.get("last").asDouble());
                        break;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return entryPrices;
    }
}

package com.example.parser_bithumb_bot.providers.bithumb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashSet;
import java.util.Set;

@Service
public class BithumbService {

    private static final String API_URL = "https://api.bithumb.com/public/ticker/ALL";

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    public Set<String> fetchPairs() throws Exception {
        String resp = rest.getForObject(API_URL, String.class);
        JsonNode data = mapper.readTree(resp).get("data");

        Set<String> pairs = new HashSet<>();
        data.fieldNames().forEachRemaining(pairs::add);
        pairs.remove("date");
        return pairs;
    }
}

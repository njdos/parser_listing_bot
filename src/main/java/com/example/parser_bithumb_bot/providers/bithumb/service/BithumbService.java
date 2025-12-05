package com.example.parser_bithumb_bot.providers.bithumb.service;

import com.example.parser_bithumb_bot.app.mapper.Mapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class BithumbService {

    private static final String API_URL = "https://api.bithumb.com/public/ticker/ALL";

    @Autowired
    private Mapper mapper;

    public Set<String> fetchPairs() throws Exception {
        String resp = mapper.getRestTemplate().getForObject(API_URL, String.class);
        JsonNode data = mapper.getObjectMapper().readTree(resp).get("data");

        Set<String> pairs = new HashSet<>();
        data.fieldNames().forEachRemaining(pairs::add);
        pairs.remove("date");
        return pairs;
    }
}

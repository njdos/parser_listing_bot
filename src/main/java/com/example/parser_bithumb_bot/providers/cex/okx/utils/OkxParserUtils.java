package com.example.parser_bithumb_bot.providers.cex.okx.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

public class OkxParserUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String extractTargetValue(String response, String targetValue) {
        try {
            var node = objectMapper.readTree(response);
            return node.path("data").get(0).path(targetValue).asText();
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract " + targetValue, e);
        }
    }

    public static BigDecimal fastExtractPrice(String msg) {
        int idx = msg.indexOf("\"px\":\"");
        if (idx == -1) return null;

        int start = idx + 6;
        int end = msg.indexOf("\"", start);

        if (end == -1) return null;

        return new BigDecimal(msg.substring(start, end));
    }

}

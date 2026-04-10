package com.example.parser_bithumb_bot.providers.cex.okx.utils;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class OkxTimeUtils {

    public static String getTimestamp() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(java.time.ZoneOffset.UTC)
                .format(Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
    }

}

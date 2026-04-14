package com.example.parser_bithumb_bot.providers.cex.okx.dto;

import java.math.BigDecimal;

public record ListingContext(
        String instId,
        BigDecimal size,
        int leverage,
        Instrument instrument
) {}
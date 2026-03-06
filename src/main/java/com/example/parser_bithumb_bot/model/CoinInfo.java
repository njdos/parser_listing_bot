package com.example.parser_bithumb_bot.model;

import java.util.List;

public record CoinInfo(
        String coinName,
        String network,
        List<TopHolder> topHolders
) {
}

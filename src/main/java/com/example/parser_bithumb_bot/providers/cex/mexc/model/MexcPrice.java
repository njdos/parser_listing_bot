package com.example.parser_bithumb_bot.providers.cex.mexc.model;

import lombok.Data;

@Data
public class MexcPrice {
    private String symbol;
    private String price;
}
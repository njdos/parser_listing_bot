package com.example.parser_bithumb_bot.providers.cex.gate.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GateOrderRequest {
    private String contract;
    private int size;
    private String price;
    private String tif;
}
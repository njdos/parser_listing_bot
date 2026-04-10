package com.example.parser_bithumb_bot.providers.cex.bingx.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BingXOrderRequest {
    private String symbol;
    private String side;
    private String positionSide;
    private String type;
    private String quantity;
    private String price;

}

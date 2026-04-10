package com.example.parser_bithumb_bot.providers.cex.okx.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class Instrument {

    private BigDecimal ctVal;
    private BigDecimal minSz;
    private BigDecimal maxMktSz;
    private BigDecimal lotSz;

    private BigDecimal longQuota;
    private BigDecimal shortQuota;

    private BigDecimal maxLeverage;
}
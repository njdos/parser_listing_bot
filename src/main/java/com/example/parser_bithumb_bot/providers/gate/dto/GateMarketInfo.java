package com.example.parser_bithumb_bot.providers.gate.dto;

public class GateMarketInfo {
    private String contract;
    private double last;
    private double markPrice;
    private double indexPrice;
    private int lowestSize;
    private double fundingRateIndicative;
    private int quantoMultiplier;

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public double getLast() {
        return last;
    }

    public void setLast(double last) {
        this.last = last;
    }

    public double getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(double markPrice) {
        this.markPrice = markPrice;
    }

    public double getIndexPrice() {
        return indexPrice;
    }

    public void setIndexPrice(double indexPrice) {
        this.indexPrice = indexPrice;
    }

    public int getLowestSize() {
        return lowestSize;
    }

    public void setLowestSize(int lowestSize) {
        this.lowestSize = lowestSize;
    }

    public double getFundingRateIndicative() {
        return fundingRateIndicative;
    }

    public void setFundingRateIndicative(double fundingRateIndicative) {
        this.fundingRateIndicative = fundingRateIndicative;
    }

    public int getQuantoMultiplier() {
        return quantoMultiplier;
    }

    public void setQuantoMultiplier(int quantoMultiplier) {
        this.quantoMultiplier = quantoMultiplier;
    }
}

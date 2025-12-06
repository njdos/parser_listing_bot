package com.example.parser_bithumb_bot.providers.gate.dto;

public class GateTickerInfo {
    private String contract;
    private double last;
    private double lowestSize;
    private double highestSize;
    private double quantoMultiplier;
    private double indexPrice;
    private double markPrice;
    private double fundingRateIndicative;

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

    public double getLowestSize() {
        return lowestSize;
    }

    public void setLowestSize(double lowestSize) {
        this.lowestSize = lowestSize;
    }

    public double getHighestSize() {
        return highestSize;
    }

    public void setHighestSize(double highestSize) {
        this.highestSize = highestSize;
    }

    public double getQuantoMultiplier() {
        return quantoMultiplier;
    }

    public void setQuantoMultiplier(double quantoMultiplier) {
        this.quantoMultiplier = quantoMultiplier;
    }

    public double getIndexPrice() {
        return indexPrice;
    }

    public void setIndexPrice(double indexPrice) {
        this.indexPrice = indexPrice;
    }

    public double getMarkPrice() {
        return markPrice;
    }

    public void setMarkPrice(double markPrice) {
        this.markPrice = markPrice;
    }

    public double getFundingRateIndicative() {
        return fundingRateIndicative;
    }

    public void setFundingRateIndicative(double fundingRateIndicative) {
        this.fundingRateIndicative = fundingRateIndicative;
    }
}
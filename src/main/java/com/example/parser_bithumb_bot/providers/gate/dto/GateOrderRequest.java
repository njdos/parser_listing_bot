package com.example.parser_bithumb_bot.providers.gate.dto;

public class GateOrderRequest {
    private String contract;
    private String side;
    private String size;
    private String price;
    private String leverage;
//    private double stopLossPrice;
//    private double takeProfitPrice;
    private String tif;
    private String text;
    private String openType;

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getLeverage() {
        return leverage;
    }

    public void setLeverage(String leverage) {
        this.leverage = leverage;
    }

//    public double getStopLossPrice() {
//        return stopLossPrice;
//    }
//
//    public void setStopLossPrice(double stopLossPrice) {
//        this.stopLossPrice = stopLossPrice;
//    }
//
//    public double getTakeProfitPrice() {
//        return takeProfitPrice;
//    }
//
//    public void setTakeProfitPrice(double takeProfitPrice) {
//        this.takeProfitPrice = takeProfitPrice;
//    }

    public String getTif() {
        return tif;
    }

    public void setTif(String tif) {
        this.tif = tif;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getOpenType() {
        return openType;
    }

    public void setOpenType(String openType) {
        this.openType = openType;
    }
}

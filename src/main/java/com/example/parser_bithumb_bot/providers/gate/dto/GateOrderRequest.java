package com.example.parser_bithumb_bot.providers.gate.dto;

public class GateOrderRequest {
    private String contract;
    private String price;
    private String size;
    private String tif;
    private String text;
    private String side;
    private String stop_loss_price;
    private String take_profit_price;

    public String getContract() {
        return contract;
    }

    public void setContract(String contract) {
        this.contract = contract;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

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

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
        this.side = side;
    }

    public String getStop_loss_price() {
        return stop_loss_price;
    }

    public void setStop_loss_price(String stop_loss_price) {
        this.stop_loss_price = stop_loss_price;
    }

    public String getTake_profit_price() {
        return take_profit_price;
    }

    public void setTake_profit_price(String take_profit_price) {
        this.take_profit_price = take_profit_price;
    }
}

package com.example.parser_bithumb_bot.app.model;

import lombok.Data;

import java.util.List;

@Data
public class ListingEvent {

    private String shortName;
    private ActionType action;
    private String source;
    private String rawText;

    public enum ActionType {
        LISTING,
        BINANCE_DELIST,
        BINANCE_FUTURES
    }

    public void setShortName(String shortName) {
        this.shortName = shortName.toUpperCase();
    }

}
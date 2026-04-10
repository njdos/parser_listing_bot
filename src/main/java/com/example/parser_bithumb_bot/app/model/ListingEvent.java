package com.example.parser_bithumb_bot.app.model;

import lombok.Data;

import java.util.List;

@Data
public class ListingEvent {

    private String shortName;
    private String action;
    private String source;
    private String rawText;
    private boolean isActive;
    private boolean contractSize;

    public void setShortName(String shortName) {
        this.shortName = shortName.toUpperCase();
    }

    public void setActive(List<String> targetSource, String targetAction) {
        this.isActive = targetSource.contains(source) && targetAction.equals(action);
    }
}
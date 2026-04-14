package com.example.parser_bithumb_bot.providers.telegram;

import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.app.model.ListingEvent.ActionType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ListingParser {

    public static List<ListingEvent> parse(String text) {
        text = text.toLowerCase();

        List<ListingEvent> events = new ArrayList<>();

        String source = extractSource(text);
        List<String> tokens = extractTokens(text);
        ActionType action = detectAction(text, source);

        if (tokens.isEmpty() || action == null) {
            return events;
        }

        for (String token : tokens) {
            ListingEvent event = new ListingEvent();
            event.setShortName(token);
            event.setSource(source);
            event.setAction(action);
            event.setRawText(text);

            events.add(event);
        }

        return events;
    }

    private static String extractSource(String text) {
        Matcher matcher = Pattern.compile("\\[(.*?)\\]").matcher(text);
        if (matcher.find()) {
            return matcher.group(1).toLowerCase();
        }
        return "";
    }

    private static List<String> extractTokens(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\$(\\w+)").matcher(text);

        while (matcher.find()) {
            tokens.add(matcher.group(1));
        }

        return tokens;
    }

    private static ActionType detectAction(String text, String source) {

        if ((source.equals("bithumb") || source.equals("upbit")) && text.contains("listed")) {
            return ActionType.LISTING;
        }

        if (source.equals("binance") && text.contains("delist")) {
            return ActionType.BINANCE_DELIST;
        }

        if (source.equals("binance") && text.contains("futures") && text.contains("launch")) {
            return ActionType.BINANCE_FUTURES;
        }

        return null;
    }
}

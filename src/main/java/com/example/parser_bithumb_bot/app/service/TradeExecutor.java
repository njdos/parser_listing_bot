package com.example.parser_bithumb_bot.app.service;

import com.example.parser_bithumb_bot.app.model.ListingEvent;
import reactor.core.publisher.Mono;

public interface TradeExecutor {
    Mono<Void> execute(ListingEvent event);

}
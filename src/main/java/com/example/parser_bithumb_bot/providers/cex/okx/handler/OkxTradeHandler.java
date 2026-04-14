package com.example.parser_bithumb_bot.providers.cex.okx.handler;

import com.example.parser_bithumb_bot.app.model.ListingEvent;
import reactor.core.publisher.Mono;

public interface OkxTradeHandler {

    boolean supports(ListingEvent.ActionType action);

    Mono<Void> handle(ListingEvent event);

}
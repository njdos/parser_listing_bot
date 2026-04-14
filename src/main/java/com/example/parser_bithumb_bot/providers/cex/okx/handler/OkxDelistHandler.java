package com.example.parser_bithumb_bot.providers.cex.okx.handler;

import com.example.parser_bithumb_bot.app.model.ListingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OkxDelistHandler implements OkxTradeHandler {

    @Override
    public boolean supports(ListingEvent.ActionType action) {
        return action == ListingEvent.ActionType.BINANCE_DELIST;
    }

    @Override
    public Mono<Void> handle(ListingEvent event) {

        String instId = event.getShortName() + "-USDT-SWAP";

        return Mono.empty();
    }
}
package com.example.parser_bithumb_bot.providers.cex.okx.handler;

import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.app.service.TradeExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OkxListingTradeProcessor implements TradeExecutor {

    private final List<OkxTradeHandler> handlers;

    @Override
    public Mono<Void> execute(ListingEvent event) {
        return Flux.fromIterable(handlers)
                .filter(handler -> handler.supports(event.getAction()))
                .next()
                .flatMap(handler -> handler.handle(event))
                .switchIfEmpty(Mono.fromRunnable(() ->
                        System.out.println("NO HANDLER FOR: " + event.getAction())
                ));
    }

}
package com.example.parser_bithumb_bot.app.core;

import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.app.service.TradeExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TradeDispatcher {

    private final List<TradeExecutor> executors;

    public Mono<Void> dispatch(ListingEvent event) {
        return Flux.fromIterable(executors)
                .flatMap(executor -> executor.execute(event))
                .then();
    }

}
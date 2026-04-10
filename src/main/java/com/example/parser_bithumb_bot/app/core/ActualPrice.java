package com.example.parser_bithumb_bot.app.core;

import com.example.parser_bithumb_bot.providers.cex.mexc.MexcAPI;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ActualPrice {

    private final MexcAPI mexcAPI;

    private volatile Map<String, Double> cache = new ConcurrentHashMap<>();

    public Mono<Void> updatePrices() {
        return mexcAPI.getAllPrices()
                .onErrorResume(e -> {
                    System.out.println("PRICE UPDATE ERROR: " + e.getMessage());
                    return Mono.empty();
                })
                .map(prices -> {
                    Map<String, Double> newMap = new ConcurrentHashMap<>();

                    prices.forEach((symbol, price) -> {
                        String normalized = normalize(symbol);
                        if (normalized != null) {
                            newMap.put(normalized, price);
                        }
                    });

                    return newMap;
                })
                .doOnNext(newMap -> {
                    if (!newMap.isEmpty()) {
                        cache = newMap;
                    } else {
                        System.out.println("SKIP EMPTY PRICE UPDATE");
                    }
                })
                .onErrorResume(e -> {
                    System.out.println("CACHE ERROR: " + e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    public Double getPrice(String symbol) {
        return cache.get(symbol);
    }

    public Set<String> getTokensShortName() {
        return cache.keySet();
    }

    private String normalize(String symbol) {
        if (symbol.endsWith("USDT")) {
            return symbol.substring(0, symbol.length() - 4).toUpperCase();
        }
        return null;
    }
}
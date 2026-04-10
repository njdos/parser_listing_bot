package com.example.parser_bithumb_bot.providers.cex.mexc.service;

import com.example.parser_bithumb_bot.providers.cex.mexc.model.MexcPrice;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Map;

@Service
public class MexcService {

    private final WebClient webClient;

    public MexcService(@Qualifier("mexcWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Map<String, Double>> getAllPrices() {
        return webClient.get()
                .uri("/api/v3/ticker/price")
                .retrieve()
                .bodyToFlux(MexcPrice.class)
                .timeout(Duration.ofSeconds(5))
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(1))
                )
                .collectMap(
                        MexcPrice::getSymbol,
                        p -> Double.parseDouble(p.getPrice())
                )
                .onErrorResume(e -> {
                    System.out.println("MEXC ERROR: " + e.getMessage());
                    return Mono.empty();
                });
    }
}
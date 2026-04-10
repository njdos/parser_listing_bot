package com.example.parser_bithumb_bot.providers.cex.gate.service;

import com.example.parser_bithumb_bot.providers.cex.gate.config.GateCredentials;
import com.example.parser_bithumb_bot.providers.cex.gate.dto.GateOrderRequest;
import com.example.parser_bithumb_bot.providers.cex.gate.utils.GateSignatureUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class GateOrderService {

    private final WebClient webClient;

    public GateOrderService(@Qualifier("gateWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> placeOrder(GateOrderRequest req) {

        long timestamp = System.currentTimeMillis() / 1000;

        String path = "/orders";

        String body = "{"
                + "\"contract\":\"" + req.getContract() + "\","
                + "\"size\":" + req.getSize() + ","
                + "\"price\":" + req.getPrice() + ","
                + "\"tif\":\"" + req.getTif() + "\""
                + "}";

        String sign = GateSignatureUtils.sign(
                "POST",
                path,
                "",
                body,
                timestamp,
                GateCredentials.SECRET_KEY
        );

        return webClient.post()
                .uri("/api/v4/futures/usdt" + path)
                .header("KEY", GateCredentials.API_KEY)
                .header("Timestamp", String.valueOf(timestamp))
                .header("SIGN", sign)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> setLeverage(String contract, int leverage) {

        long timestamp = System.currentTimeMillis() / 1000;

        String path = "/positions/" + contract + "/leverage";

        String query = "leverage=" + leverage;

        String sign = GateSignatureUtils.sign(
                "POST",
                path,
                query,
                "",
                timestamp,
                GateCredentials.SECRET_KEY
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v4/futures/usdt" + path)
                        .queryParam("leverage", leverage)
                        .build())
                .header("KEY", GateCredentials.API_KEY)
                .header("Timestamp", String.valueOf(timestamp))
                .header("SIGN", sign)
                .retrieve()
                .bodyToMono(String.class);
    }
}
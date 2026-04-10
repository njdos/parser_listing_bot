package com.example.parser_bithumb_bot.providers.cex.bingx.service;

import com.example.parser_bithumb_bot.providers.cex.bingx.config.BingxCredentials;
import com.example.parser_bithumb_bot.providers.cex.bingx.dto.BingXOrderRequest;
import com.example.parser_bithumb_bot.providers.cex.bingx.utils.SignatureUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class BingxOrderService {

    private final WebClient webClient;

    public BingxOrderService(@Qualifier("bingxWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<String> placeOrder(BingXOrderRequest req) {
            long timestamp = System.currentTimeMillis();

        String query = new StringBuilder()
                .append("symbol=").append(req.getSymbol())
                .append("&side=").append(req.getSide())
                .append("&positionSide=").append(req.getPositionSide())
                .append("&type=").append(req.getType())
                .append("&quantity=").append(req.getQuantity())
                .append("&timestamp=").append(timestamp)
                .toString();

        if (req.getPrice() != null) {
            query += "&price=" + req.getPrice();
        }

        String signature = SignatureUtils.sign(query, BingxCredentials.SECRET_KEY);

        String finalQuery = query;
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/openApi/swap/v2/trade/order")
                        .query(finalQuery + "&signature=" + signature)
                        .build())
                .header("X-BX-APIKEY", BingxCredentials.API_KEY)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> setLeverage(String symbol, int leverage, String side) {
        long timestamp = System.currentTimeMillis();

        String query = new StringBuilder()
                .append("symbol=").append(symbol)
                .append("&leverage=").append(leverage)
                .append("&side=").append(side)
                .append("&timestamp=").append(timestamp)
                .toString();

        String signature = SignatureUtils.sign(query, BingxCredentials.SECRET_KEY);

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/openApi/swap/v2/trade/leverage")
                        .query(query + "&signature=" + signature)
                        .build())
                .header("X-BX-APIKEY", BingxCredentials.API_KEY)
                .retrieve()
                .bodyToMono(String.class);
    }

}
package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.providers.cex.okx.config.OkxCredentials;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxParserUtils;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxSignatureUtil;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxTimeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Service
public class OkxOrderRequestService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OkxOrderRequestService(@Qualifier("okxWebClient") WebClient webClient) {
        this.webClient = webClient;
    }

    @PostConstruct
    public void init() {
        setHedgeMode().subscribe();
    }

    public Mono<String> getInstrumentInfo(String instId) {
        return webClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v5/public/instruments")
                        .queryParam("instType", "SWAP")
                        .queryParam("instId", instId)
                        .build()
                )
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> setLeverage(String instId, int leverage) {
        String path = "/api/v5/account/set-leverage";

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("instId", instId);
        bodyMap.put("lever", String.valueOf(leverage));
        bodyMap.put("mgnMode", "isolated");
        bodyMap.put("posSide", "long");

        return request("POST", path, bodyMap);
    }

    public Mono<String> placeMarketOrder(String instId, String size) {
        return request("POST", "/api/v5/trade/order", Map.of(
                "instId", instId,
                "tdMode", "isolated",
                "side", "buy",
                "posSide", "long",
                "ordType", "market",
                "sz", size
        ));
    }

    public Mono<String> closePosition(String instId, String size) {
        return request("POST", "/api/v5/trade/order", Map.of(
                "instId", instId,
                "tdMode", "isolated",
                "side", "sell",
                "posSide", "long",
                "ordType", "market",
                "sz", size
        ));
    }

    // ================= SHORT =================

    public Mono<String> openShort(String instId, String size) {
        return request("POST", "/api/v5/trade/order", Map.of(
                "instId", instId,
                "tdMode", "isolated",
                "side", "sell",
                "posSide", "short",
                "ordType", "market",
                "sz", size
        ));
    }

    public Mono<String> closeShort(String instId, String size) {
        return request("POST", "/api/v5/trade/order", Map.of(
                "instId", instId,
                "tdMode", "isolated",
                "side", "buy",
                "posSide", "short",
                "ordType", "market",
                "reduceOnly", true,
                "sz", size
        ));
    }

    private Mono<String> setHedgeMode() {
        String path = "/api/v5/account/set-position-mode";
        String body = """
                {
                  "posMode": "long_short_mode"
                }""";

        String timestamp = OkxTimeUtils.getTimestamp();
        String sign = OkxSignatureUtil.sign(
                timestamp, "POST", path, body, OkxCredentials.SECRET_KEY);

        return webClient
                .post()
                .uri(path)
                .header("OK-ACCESS-KEY", OkxCredentials.API_KEY)
                .header("OK-ACCESS-SIGN", sign)
                .header("OK-ACCESS-TIMESTAMP", timestamp)
                .header("OK-ACCESS-PASSPHRASE", OkxCredentials.PASSPHRASE)
                .header("Content-Type", "application/json")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class);
    }

    private Mono<String> request(String method, String path, Map<String, Object> bodyMap) {
        try {
            String timestamp = OkxTimeUtils.getTimestamp();
            String upperMethod = method.toUpperCase();

            String body = objectMapper.writeValueAsString(bodyMap);

            String sign = OkxSignatureUtil.sign(
                    timestamp, upperMethod, path, body, OkxCredentials.SECRET_KEY);

            return webClient
                    .method(HttpMethod.valueOf(upperMethod))
                    .uri(path)
                    .header("OK-ACCESS-KEY", OkxCredentials.API_KEY)
                    .header("OK-ACCESS-SIGN", sign)
                    .header("OK-ACCESS-TIMESTAMP", timestamp)
                    .header("OK-ACCESS-PASSPHRASE", OkxCredentials.PASSPHRASE)
                    .header("Content-Type", "application/json")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnNext(resp -> System.out.println(" ----- PATH: " + path + " BODY: " + body + " RESPONSE: " + resp));
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

}
package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.providers.cex.okx.dto.ListingContext;
import com.example.parser_bithumb_bot.providers.cex.okx.http.OkxOrderRequestService;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxParserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class ListingService {

    private final @Qualifier("okxWsClient") WebSocketClient wsClient;
    private final OkxOrderRequestService requestService;

    private static final String WS_URL = "wss://ws.okx.com:8443/ws/v5/public";

    private static final double[] LEVELS = {0.01, 0.02, 0.03, 0.04, 0.05};
    private static final double[] CLOSE_PARTS = {0.20, 0.20, 0.20, 0.20, 0.20};

    public Mono<Void> execute(ListingContext ctx) {

        return requestService.setLeverage(ctx.instId(), ctx.leverage(), true)
                .then(requestService.placeMarketOrder(ctx.instId(), ctx.size().toPlainString()))
                .map(resp -> OkxParserUtils.extractTargetValue(resp, "ordId"))
                .doOnNext(orderId -> System.out.println("ORDER CREATED: " + orderId))
                .flatMapMany(orderId -> {

                    AtomicReference<BigDecimal> entryPrice = new AtomicReference<>(BigDecimal.ZERO);
                    AtomicReference<BigDecimal> maxPrice = new AtomicReference<>(BigDecimal.ZERO);
                    AtomicReference<BigDecimal> remaining = new AtomicReference<>(ctx.size());

                    AtomicReference<Boolean> isShort = new AtomicReference<>(false);

                    boolean[] triggered = new boolean[LEVELS.length];

                    String subscribe = """
                            {
                              "op": "subscribe",
                              "args": [{
                                "channel": "trades",
                                "instId": "%s"
                              }]
                            }
                            """.formatted(ctx.instId());

                    return Mono.defer(() ->
                                    wsClient.execute(URI.create(WS_URL), session -> {
                                        Mono<Void> sendSubscribe = session.send(Mono.just(session.textMessage(subscribe)));
                                        Mono<Void> sendPing = session.send(Flux.interval(Duration.ofSeconds(20))
                                                .map(i -> session.textMessage("ping")));

                                        Mono<Void> receive = session.receive()
                                                .map(msg -> msg.getPayloadAsText())
                                                .filter(p -> p.contains("\"data\"") && p.contains("\"px\""))
                                                .map(OkxParserUtils::fastExtractPrice)
                                                .filter(Objects::nonNull)
                                                .flatMap(price -> {

                                                    String typeSide = isShort.get() ? "SHORT" : "LONG";

                                                    System.out.println("--------------------------------");
                                                    System.out.println("ENTRY: " + entryPrice.get());
                                                    System.out.println("MAX: " + maxPrice.get());
                                                    System.out.println("CURRENT: " + price);
                                                    System.out.println("TYPE SIDE: " + typeSide);
                                                    System.out.println("--------------------------------");

                                                    if (entryPrice.compareAndSet(BigDecimal.ZERO, price)) {
                                                        maxPrice.set(price);
                                                        System.out.println("ENTRY: " + price + " | " + typeSide);
                                                        return Mono.empty();
                                                    }

                                                    BigDecimal entry = entryPrice.get();

                                                    if (!isShort.get()) {
                                                        if (price.compareTo(maxPrice.get()) > 0) {
                                                            maxPrice.set(price);
                                                        }
                                                    } else {
                                                        if (price.compareTo(maxPrice.get()) < 0) {
                                                            maxPrice.set(price);
                                                        }
                                                    }

                                                    double profit = calculateProfit(entry, price, ctx.leverage(), isShort.get());

                                                    System.out.println("PRICE: " + price + " PROFIT: " + profit + " " + typeSide);

                                                    // ==============================
                                                    // 🔥 REVERSAL LOGIC
                                                    // ==============================
                                                    if (!isShort.get() && profit <= -TradingConfig.REVERSAL_DROP) {

                                                        System.out.println("🔄 REVERSAL → SHORT");

                                                        return closeAll(ctx.instId(), remaining, "REVERSAL")
                                                                .then(requestService.setLeverage(ctx.instId(), ctx.leverage(), false))
                                                                .then(openShort(ctx.instId(), ctx.size()))
                                                                .doOnSuccess(v -> {
                                                                    isShort.set(true);
                                                                    resetState(entryPrice, maxPrice, remaining, ctx.size(), triggered);
                                                                });
                                                    }

                                                    // ==============================
                                                    // PARTIAL CLOSE
                                                    // ==============================
                                                    for (int i = 0; i < LEVELS.length; i++) {

                                                        if (triggered[i]) continue;
                                                        if (profit < LEVELS[i]) continue;

                                                        triggered[i] = true;

                                                        BigDecimal toClose = calculateClose(ctx.size(), remaining, i);

                                                        if (toClose.compareTo(BigDecimal.ZERO) <= 0) continue;
                                                        if (toClose.compareTo(ctx.instrument().getMinSz()) < 0) continue;

                                                        BigDecimal normalized = normalizeSize(toClose, ctx.instrument().getLotSz());
                                                        BigDecimal finalSize = safeClamp(remaining, normalized);

                                                        if (finalSize.compareTo(BigDecimal.ZERO) <= 0) continue;

                                                        remaining.updateAndGet(r -> r.subtract(finalSize));

                                                        System.out.println(
                                                                "💰 PARTIAL CLOSE | " +
                                                                        "INST: " + ctx.instId() +
                                                                        " | LEVEL: " + i +
                                                                        " | TARGET: " + LEVELS[i] * 100 +
                                                                        " | CLOSED: " + CLOSE_PARTS[i] * 100 + "%" +
                                                                        " | SIZE: " + finalSize +
                                                                        " | REMAINING: " + remaining.get() +
                                                                        " | PRICE: " + price +
                                                                        " | ENTRY: " + entry +
                                                                        " | PROFIT: " + profit +
                                                                        " | SIDE: " + typeSide
                                                        );

                                                        return requestService.closePosition(ctx.instId(), finalSize.toPlainString())
                                                                .onErrorResume(e -> Mono.empty());
                                                    }

                                                    // ==============================
                                                    // TRAILING
                                                    // ==============================
                                                    BigDecimal trigger;

                                                    if (!isShort.get()) {
                                                        trigger = maxPrice.get()
                                                                .multiply(BigDecimal.valueOf(1 - TradingConfig.DROP_HIGH));

                                                        if (price.compareTo(trigger) < 0) {
                                                            return closeAll(ctx.instId(), remaining, "EXIT LONG");
                                                        }
                                                    } else {
                                                        trigger = maxPrice.get()
                                                                .multiply(BigDecimal.valueOf(1 + TradingConfig.DROP_HIGH));

                                                        if (price.compareTo(trigger) > 0) {
                                                            return closeAll(ctx.instId(), remaining, "EXIT SHORT");
                                                        }
                                                    }

                                                    return Mono.empty();
                                                })
                                                .then();

                                        return Mono.when(sendSubscribe, sendPing, receive);
                                    })
                            )
                            .repeat()
                            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3)));

                })
                .then();
    }


    // ================= LOGIC =================

    private double calculateProfit(BigDecimal entry,
                                   BigDecimal price,
                                   int leverage,
                                   boolean isShort) {

        BigDecimal change;

        if (!isShort) {
            change = price.subtract(entry).divide(entry, 6, RoundingMode.HALF_UP);
        } else {
            change = entry.subtract(price).divide(entry, 6, RoundingMode.HALF_UP);
        }

        return change.multiply(BigDecimal.valueOf(leverage)).doubleValue();
    }

    private void resetState(AtomicReference<BigDecimal> entry,
                            AtomicReference<BigDecimal> max,
                            AtomicReference<BigDecimal> remaining,
                            BigDecimal fullSize,
                            boolean[] triggered) {

        entry.set(BigDecimal.ZERO);
        max.set(BigDecimal.ZERO);
        remaining.set(fullSize);
        Arrays.fill(triggered, false);
    }

    private Mono<String> openShort(String instId, BigDecimal size) {
        System.out.println("OPEN SHORT: " + size);
        return requestService.openShort(instId, size.toPlainString());
    }

    private BigDecimal calculateClose(BigDecimal fullSize,
                                      AtomicReference<BigDecimal> remaining,
                                      int index) {

        double part = 0;

        for (int j = 0; j <= index; j++) {
            part += CLOSE_PARTS[j];
        }

        BigDecimal target = fullSize.multiply(BigDecimal.valueOf(part));
        BigDecimal closed = fullSize.subtract(remaining.get());

        return target.subtract(closed);
    }

    private BigDecimal normalizeSize(BigDecimal size, BigDecimal lotSz) {
        if (lotSz == null || lotSz.compareTo(BigDecimal.ZERO) == 0) return size;

        return size.divide(lotSz, 0, RoundingMode.DOWN).multiply(lotSz);
    }

    private BigDecimal safeClamp(AtomicReference<BigDecimal> remaining, BigDecimal size) {
        return remaining.get().min(size);
    }

    private Mono<String> closeAll(String instId,
                                  AtomicReference<BigDecimal> remaining,
                                  String reason) {

        BigDecimal rest = remaining.get();

        if (rest.compareTo(BigDecimal.ZERO) <= 0) return Mono.empty();

        remaining.set(BigDecimal.ZERO);

        System.out.println(reason + " CLOSE ALL: " + rest);

        return requestService.closePosition(instId, rest.toPlainString())
                .onErrorResume(e -> Mono.empty());
    }

}
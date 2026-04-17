package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.providers.cex.okx.dto.ListingContext;
import com.example.parser_bithumb_bot.providers.cex.okx.http.OkxOrderRequestService;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxParserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingService {

    private final @Qualifier("okxWsClient") WebSocketClient wsClient;
    private final OkxOrderRequestService requestService;

    private static final String WS_URL = "wss://ws.okx.com:8443/ws/v5/public";

    public Mono<Void> execute(ListingContext ctx) {

        return requestService.setLeverage(ctx.instId(), ctx.leverage(), false)
                .then(requestService.placeMarketOrder(ctx.instId(), ctx.size().toPlainString(), false))
                .doOnNext(resp -> log.info("{} 🚀 ORDER CREATED ", ctx.instId()))
                .flatMapMany(x -> runWs(ctx))
                .then();
    }

    private Flux<Void> runWs(ListingContext ctx) {

        AtomicReference<BigDecimal> entry = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> max = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> remaining = new AtomicReference<>(ctx.size());
        AtomicBoolean isShort = new AtomicBoolean(false);

        boolean[] triggered = new boolean[TradingConfig.PROFIT_LEVELS.length];

        AtomicBoolean active = new AtomicBoolean(true);
        AtomicBoolean finished = new AtomicBoolean(false);

        String shortNameCoin = "$" + ctx.instId().replace("-USDT-SWAP", "");

        String subscribe = """
                {
                  "op": "subscribe",
                  "args": [{
                    "channel": "trades",
                    "instId": "%s"
                  }]
                }
                """.formatted(ctx.instId());

        return Flux.defer(() ->
                        wsClient.execute(URI.create(WS_URL), session -> {

                            Mono<Void> sendSubscribe =
                                    session.send(Mono.just(session.textMessage(subscribe))
                                            .doOnSuccess(s -> log.warn("{} ✅ STRATEGY STARTED", shortNameCoin)));

                            // ✅ ПІНГ ЗАЛИШИВ
                            Mono<Void> sendPing =
                                    session.send(Flux.interval(Duration.ofSeconds(20))
                                            .map(i -> session.textMessage("ping")));

                            Mono<Void> receive = session.receive()
                                    .map(msg -> msg.getPayloadAsText())
                                    .filter(p -> active.get())
                                    .filter(p -> p.contains("\"data\"") && p.contains("\"px\""))
                                    .map(OkxParserUtils::fastExtractPrice)
                                    .filter(Objects::nonNull)
                                    .concatMap(price -> {

                                        String side = isShort.get() ? "SHORT" : "LONG";

                                        // ENTRY
                                        if (entry.compareAndSet(BigDecimal.ZERO, price)) {
                                            max.set(price);
                                            log.info("{} 🎯 ENTRY | {}", shortNameCoin, price);
                                            return Mono.empty();
                                        }

                                        // UPDATE MAX / MIN
                                        if (!isShort.get()) {
                                            if (price.compareTo(max.get()) > 0) {
                                                max.set(price);
                                            }
                                        } else {
                                            if (price.compareTo(max.get()) < 0) {
                                                max.set(price);
                                            }
                                        }

                                        double pnl = calculatePnlPercent(entry.get(), price, ctx.leverage(), isShort.get());
                                        log.info("{} [{}] price={} entry={} max={} pnl={}%", shortNameCoin, side, price, entry.get(), max.get(), pnl);

                                        if (pnl < -TradingConfig.REVERSAL_DROP) {
                                            log.info("\uD83D\uDD3B CLOSE ALL coin: {}, side: {}, pnl: {}%", shortNameCoin, side, pnl);

                                            if (!isShort.get()) {
                                                return closeAll(ctx.instId(), remaining, isShort.get())
                                                        .then(requestService.setLeverage(ctx.instId(), ctx.leverage(), true))
                                                        .then(requestService.placeMarketOrder(ctx.instId(), ctx.size().toPlainString(), true))
                                                        .doOnSuccess(v -> {
                                                            isShort.set(true);
                                                            entry.set(BigDecimal.ZERO);
                                                            max.set(BigDecimal.ZERO);
                                                            remaining.set(ctx.size());
                                                            Arrays.fill(triggered, false);
                                                        });
                                            } else {
                                                return closeAll(ctx.instId(), remaining, isShort.get())
                                                        .doOnSuccess(v -> {
                                                            active.set(false);
                                                            finished.set(true);
                                                            log.warn("{} 🛑 STRATEGY FINISHED", shortNameCoin);
                                                        });
                                            }
                                        }

                                        int maxLevelIndex = -1;

                                        for (int i = 0; i < TradingConfig.PROFIT_LEVELS.length; i++) {
                                            if (pnl >= TradingConfig.PROFIT_LEVELS[i]) {
                                                maxLevelIndex = i;
                                            }
                                        }

                                        if (maxLevelIndex >= 0) {

                                            BigDecimal totalToClose = BigDecimal.ZERO;

                                            for (int i = 0; i <= maxLevelIndex; i++) {
                                                if (triggered[i]) continue;
                                                triggered[i] = true;
                                                BigDecimal part = ctx.size()
                                                        .multiply(BigDecimal.valueOf(TradingConfig.CLOSE_PARTS[i]));
                                                totalToClose = totalToClose.add(part);
                                                log.info("{} 💰 APPLY LEVEL | i={} | level={}", shortNameCoin, i, TradingConfig.PROFIT_LEVELS[i]);
                                            }

                                            if (totalToClose.compareTo(BigDecimal.ZERO) > 0) {
                                                BigDecimal finalSize = remaining.get().min(
                                                        normalizeSize(totalToClose, ctx.instrument().getLotSz())
                                                );
                                                if (finalSize.compareTo(ctx.instrument().getMinSz()) >= 0) {
                                                    remaining.updateAndGet(r -> r.subtract(finalSize));
                                                    log.info("{} 💰 CLOSE TOTAL | {} | remain={}", shortNameCoin, finalSize, remaining.get());
                                                    return requestService.closePosition(ctx.instId(), finalSize.toPlainString(), isShort.get())
                                                            .onErrorResume(e -> Mono.empty());
                                                }
                                            }
                                        }

                                        return Mono.empty();
                                    })
                                    .then();

                            return Mono.when(sendSubscribe, sendPing, receive);
                        })
                )
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3)))
                .filter(x -> !finished.get());
    }

    private Mono<Void> closeAll(String instId, AtomicReference<BigDecimal> remaining, boolean isShort) {
        BigDecimal rest = remaining.get();
        if (rest.compareTo(BigDecimal.ZERO) <= 0) return Mono.empty();
        remaining.set(BigDecimal.ZERO);
        return requestService.closePosition(instId, rest.toPlainString(), isShort).then();
    }

    private BigDecimal normalizeSize(BigDecimal size, BigDecimal lotSz) {
        if (lotSz == null || lotSz.compareTo(BigDecimal.ZERO) == 0) return size;

        return size.divide(lotSz, 0, RoundingMode.DOWN).multiply(lotSz);
    }

    private double calculatePnlPercent(BigDecimal entry, BigDecimal price, int leverage, boolean isShort) {
        BigDecimal move = isShort ? entry.subtract(price) : price.subtract(entry);

        return move
                .divide(entry, 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(leverage))
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }

}
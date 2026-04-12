package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.providers.cex.okx.dto.Instrument;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxParserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
public class OkxWsPriceService {

    private final @Qualifier("okxWsClient") WebSocketClient wsClient;
    private final OkxOrderRequestService orderService;

    private static final String WS_URL = "wss://ws.okx.com:8443/ws/v5/public";
    private static final double[] DEFAULT_LEVELS = {0.01, 0.02, 0.03, 0.04, 0.05};
    private static final double[] CLOSE_PARTS = {0.20, 0.20, 0.20, 0.20, 0.20};

    public Flux<Void> runLongSniper(String instId, BigDecimal fullSize, int leverage, Instrument inst) {

        AtomicReference<BigDecimal> maxPrice = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> entryPrice = new AtomicReference<>(BigDecimal.ZERO);
        AtomicReference<BigDecimal> remaining = new AtomicReference<>(fullSize);

        boolean[] triggered = new boolean[DEFAULT_LEVELS.length];

        String subscribe = """
                {
                  "op": "subscribe",
                  "args": [{
                    "channel": "trades",
                    "instId": "%s"
                  }]
                }
                """.formatted(instId);

        return Mono.defer(() ->
                        wsClient.execute(URI.create(WS_URL), session -> {

                            Mono<Void> sendSubscribe = session.send(
                                    Mono.just(session.textMessage(subscribe))
                            );

                            Flux<WebSocketMessage> pingFlux = Flux.interval(Duration.ofSeconds(20))
                                    .map(i -> session.textMessage("ping"));

                            Mono<Void> sendPing = session.send(pingFlux);

                            Mono<Void> receive = session.receive()
                                    .map(msg -> msg.getPayloadAsText())
                                    .filter(p -> p.contains("\"data\"") && p.contains("\"px\""))
                                    .map(OkxParserUtils::fastExtractPrice)
                                    .filter(Objects::nonNull)
                                    .flatMap(price -> {

                                        System.out.println("--------------------------------");
                                        System.out.println("ENTRY: " + entryPrice.get());
                                        System.out.println("MAX: " + maxPrice.get());
                                        System.out.println("CURRENT: " + price);
                                        System.out.println("--------------------------------");

                                        if (entryPrice.compareAndSet(BigDecimal.ZERO, price)) {
                                            maxPrice.set(price);
                                            System.out.println("ENTRY LOCKED: " + price);
                                            return Mono.empty();
                                        }

                                        BigDecimal entry = entryPrice.get();

                                        if (price.compareTo(maxPrice.get()) > 0) {
                                            maxPrice.set(price);
                                        }

                                        BigDecimal priceChange = price.subtract(entry)
                                                .divide(entry, 6, RoundingMode.HALF_UP);

                                        double profit = priceChange
                                                .multiply(BigDecimal.valueOf(leverage))
                                                .doubleValue();

                                        System.out.println("PROFIT: " + profit);

                                        // STOP LOSS
                                        if (profit <= -0.12) {
                                            return closeAll(instId, remaining, "STOP");
                                        }

                                        // PARTIAL
                                        for (int i = 0; i < DEFAULT_LEVELS.length; i++) {

                                            if (triggered[i]) continue;
                                            if (profit < DEFAULT_LEVELS[i]) continue;

                                            triggered[i] = true;

                                            BigDecimal toClose = calculateClose(fullSize, remaining, i);

                                            if (toClose.compareTo(BigDecimal.ZERO) <= 0) continue;
                                            if (toClose.compareTo(inst.getMinSz()) < 0) continue;

                                            BigDecimal normalized = normalizeSize(toClose, inst.getLotSz());
                                            BigDecimal finalSize = safeClamp(remaining, normalized);

                                            if (finalSize.compareTo(BigDecimal.ZERO) <= 0) continue;

                                            System.out.println("PARTIAL CLOSE: " + finalSize);

                                            remaining.updateAndGet(r -> r.subtract(finalSize));

                                            return orderService.closePosition(instId, finalSize.toPlainString())
                                                    .onErrorResume(e -> Mono.empty());
                                        }

                                        // TRAILING
                                        BigDecimal trigger = maxPrice.get()
                                                .multiply(BigDecimal.valueOf(1 - TradingConfig.DROP_HIGH));

                                        if (price.compareTo(trigger) < 0) {
                                            return closeAll(instId, remaining, "EXIT");
                                        }

                                        return Mono.empty();
                                    })
                                    .then();

                            return Mono.when(sendSubscribe, sendPing, receive)
                                    .doOnSubscribe(s -> System.out.println("WS CONNECTED"))
                                    .doOnError(e -> System.out.println("WS ERROR: " + e.getMessage()))
                                    .doFinally(s -> System.out.println("WS CLOSED: " + s));
                        })
                )
                .repeat()
                .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3))
                        .doBeforeRetry(r -> System.out.println("🔄 RECONNECT WS...")));
    }

    // ================= FIXED LOGIC =================

    private BigDecimal calculateClose(
            BigDecimal fullSize, AtomicReference<BigDecimal> remaining, int index) {

        boolean skipped = false;

        for (int j = 0; j < index; j++) {
            if (remaining.get().compareTo(fullSize) < 0) {
                skipped = true;
                break;
            }
        }

        double part;

        if (skipped) {
            part = CLOSE_PARTS[index];
        } else {
            part = 0;
            for (int j = 0; j <= index; j++) {
                part += CLOSE_PARTS[j];
            }
        }

        BigDecimal target = fullSize.multiply(BigDecimal.valueOf(part));

        BigDecimal alreadyClosed = fullSize.subtract(remaining.get());

        return target.subtract(alreadyClosed);
    }

    private BigDecimal normalizeSize(BigDecimal size, BigDecimal lotSz) {
        if (lotSz == null || lotSz.compareTo(BigDecimal.ZERO) == 0) {
            return size;
        }

        return size.divide(lotSz, 0, RoundingMode.DOWN)
                .multiply(lotSz);
    }

    private BigDecimal safeClamp(AtomicReference<BigDecimal> remaining, BigDecimal size) {
        BigDecimal r = remaining.get();
        return r.min(size);
    }

    private Mono<Void> closeAll(
            String instId, AtomicReference<BigDecimal> remaining, String reason) {

        BigDecimal rest = remaining.get();

        if (rest.compareTo(BigDecimal.ZERO) <= 0) {
            return Mono.empty();
        }

        remaining.set(BigDecimal.ZERO);

        return orderService.closePosition(instId, rest.toPlainString())
                .then();
    }
}
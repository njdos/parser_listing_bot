package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.providers.cex.okx.dto.Instrument;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxParserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.WebSocketMessage;
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

    private static final double[] DEFAULT_LEVELS = {0.01, 0.02, 0.05, 0.10, 0.15, 0.30, 0.50, 0.70};

    public Flux<Void> runLongSniper(String instId,
                                    BigDecimal fullSize,
                                    int leverage,
                                    Instrument inst) {

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
                        wsClient.execute(
                                URI.create(WS_URL),
                                session -> {

                                    // ✅ subscribe message
                                    Mono<Void> sendSubscribe = session.send(
                                            Mono.just(session.textMessage(subscribe))
                                    );

                                    // ✅ heartbeat (ping)
                                    Flux<WebSocketMessage> pingFlux = Flux.interval(Duration.ofSeconds(20))
                                            .map(i -> session.textMessage("ping"));

                                    Mono<Void> sendPing = session.send(pingFlux);

                                    // ✅ receive stream (MAIN LOGIC)
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

                                                // ENTRY
                                                if (entryPrice.compareAndSet(BigDecimal.ZERO, price)) {
                                                    maxPrice.set(price);
                                                    System.out.println("ENTRY LOCKED: " + price);
                                                    return Mono.empty();
                                                }

                                                BigDecimal entry = entryPrice.get();

                                                // MAX
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
                                                    return closeAll(instId, remaining, "STOP")
                                                            .onErrorResume(e -> Mono.empty());
                                                }

                                                // PARTIAL
                                                for (int i = 0; i < DEFAULT_LEVELS.length; i++) {

                                                    if (!triggered[i] && profit >= DEFAULT_LEVELS[i]) {

                                                        triggered[i] = true;

                                                        BigDecimal targetClose = getTargetClose(fullSize, DEFAULT_LEVELS[i]);
                                                        BigDecimal alreadyClosed = fullSize.subtract(remaining.get());
                                                        BigDecimal rawToClose = targetClose.subtract(alreadyClosed);

                                                        if (rawToClose.compareTo(BigDecimal.ZERO) <= 0) continue;

                                                        BigDecimal normalized = normalizeSize(rawToClose, inst.getLotSz());
                                                        BigDecimal toClose = safePart(remaining, normalized);

                                                        if (toClose.compareTo(BigDecimal.ZERO) <= 0) continue;
                                                        if (toClose.compareTo(inst.getMinSz()) < 0) continue;

                                                        System.out.println("PARTIAL CLOSE: " + toClose);

                                                        if (remaining.get().compareTo(BigDecimal.ZERO) <= 0) {
                                                            System.out.println("SKIP CLOSE → NO POSITION");
                                                            return Mono.empty();
                                                        }

                                                        BigDecimal currentRemaining = remaining.get();

                                                        if (currentRemaining.compareTo(toClose) < 0) {
                                                            toClose = currentRemaining;
                                                        }

                                                        BigDecimal finalToClose = toClose;
                                                        remaining.updateAndGet(r -> r.subtract(finalToClose));

                                                        return orderService.closePosition(instId, toClose.toPlainString())
                                                                .onErrorResume(e -> {
                                                                    if (e.getMessage() != null && e.getMessage().contains("51169")) {
                                                                        System.out.println("IGNORE 51169 (already closed)");
                                                                        return Mono.empty();
                                                                    }
                                                                    return Mono.empty();
                                                                });
                                                    }
                                                }

                                                // TRAILING
                                                BigDecimal trigger = maxPrice.get()
                                                        .multiply(BigDecimal.valueOf(1 - TradingConfig.DROP_HIGH));

                                                if (price.compareTo(trigger) < 0) {
                                                    System.out.println("TRAILING EXIT");

                                                    if (remaining.get().compareTo(BigDecimal.ZERO) <= 0) {
                                                        return Mono.empty();
                                                    }

                                                    return closeAll(instId, remaining, "EXIT")
                                                            .onErrorResume(e -> Mono.empty());
                                                }

                                                return Mono.empty();
                                            })

                                            .then();

                                    // ✅ IMPORTANT: объединяем send + receive + ping
                                    return Mono.when(sendSubscribe, sendPing, receive)
                                            .doOnSubscribe(s -> System.out.println("WS CONNECTED"))
                                            .doOnError(e -> System.out.println("WS ERROR: " + e.getMessage()))
                                            .doFinally(s -> System.out.println("WS CLOSED: " + s));
                                }
                        )
                )

                // ✅ reconnect ВСЕГДА (даже при normal close)
                .repeat()

                // ✅ retry при ошибках
                .retryWhen(
                        Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(3))
                                .doBeforeRetry(r -> System.out.println("🔄 RECONNECT WS..."))
                );
    }

    // ================= HELPERS =================

    private Mono<Void> closeAll(String instId, AtomicReference<BigDecimal> remaining, String reason) {

        BigDecimal rest = remaining.get();

        if (rest.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println(reason + " → SKIP (no position)");
            return Mono.empty();
        }

        System.out.println(reason + " → CLOSE ALL: " + rest);

        remaining.set(BigDecimal.ZERO);

        return orderService.closePosition(instId, rest.toPlainString())
                .onErrorResume(e -> {
                    if (e.getMessage() != null && e.getMessage().contains("51169")) {
                        System.out.println("IGNORE 51169 (no position)");
                        return Mono.empty();
                    }
                    return Mono.empty();
                }).then();
    }

    private BigDecimal normalizeSize(BigDecimal size, BigDecimal lotSz) {
        if (lotSz == null || lotSz.compareTo(BigDecimal.ZERO) == 0) {
            return size;
        }

        return size
                .divide(lotSz, 0, RoundingMode.DOWN)
                .multiply(lotSz);
    }

    private BigDecimal safePart(AtomicReference<BigDecimal> remaining, BigDecimal partSize) {
        BigDecimal rest = remaining.get();

        if (rest.compareTo(BigDecimal.ZERO) <= 0) return BigDecimal.ZERO;

        BigDecimal part = rest.min(partSize);

        remaining.updateAndGet(r -> r.subtract(part));

        return part;
    }

    private BigDecimal getTargetClose(BigDecimal fullSize, double level) {
        return fullSize.multiply(BigDecimal.valueOf(level));
    }
}
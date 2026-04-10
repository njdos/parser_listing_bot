package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.providers.cex.okx.dto.Instrument;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.atomic.AtomicReference;

public class PartialCloseManager {

    // уровни прибыли (profit >= level)
    private static final double[] LEVELS = {0.05, 0.10, 0.15, 0.25, 0.70};

    // сколько закрывать НА КАЖДОМ уровне (НЕ накопительно)
    private static final double[] CLOSE_PARTS = {0.20, 0.40, 0.20, 0.10, 0.10};

    private final boolean[] triggered = new boolean[LEVELS.length];

    private final AtomicReference<BigDecimal> remaining;
    private final BigDecimal fullSize;

    private final OkxOrderService orderService;

    public PartialCloseManager(BigDecimal fullSize,
                               OrderService orderService) {
        this.fullSize = fullSize;
        this.remaining = new AtomicReference<>(fullSize);
        this.orderService = orderService;
    }

    public Mono<Void> handle(String instId,
                             Instrument inst,
                             double profit) {

        for (int i = 0; i < LEVELS.length; i++) {

            if (triggered[i]) continue;
            if (profit < LEVELS[i]) continue;

            triggered[i] = true;

            BigDecimal toClose = calculateCloseAmount(i);

            if (toClose.compareTo(BigDecimal.ZERO) <= 0) continue;

            BigDecimal normalized = normalizeSize(toClose, inst.getLotSz());
            BigDecimal safe = safePart(normalized);

            if (safe.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (safe.compareTo(inst.getMinSz()) < 0) continue;

            BigDecimal currentRemaining = remaining.get();

            if (currentRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                System.out.println("SKIP CLOSE → NO POSITION");
                return Mono.empty();
            }

            if (safe.compareTo(currentRemaining) > 0) {
                safe = currentRemaining;
            }

            BigDecimal finalToClose = safe;

            remaining.updateAndGet(r -> r.subtract(finalToClose));

            System.out.println("PARTIAL CLOSE: " + finalToClose);

            return orderService.closePosition(instId, finalToClose.toPlainString())
                    .onErrorResume(e -> {
                        if (e.getMessage() != null && e.getMessage().contains("51169")) {
                            System.out.println("IGNORE 51169 (already closed)");
                            return Mono.empty();
                        }
                        return Mono.empty();
                    });
        }

        return Mono.empty();
    }

    /**
     * Главная логика:
     * - если уровни пропущены → закрываем их суммарно
     * - иначе → только текущий кусок
     */
    private BigDecimal calculateCloseAmount(int index) {

        boolean hasTriggeredBefore = false;

        for (int j = 0; j < index; j++) {
            if (triggered[j]) {
                hasTriggeredBefore = true;
                break;
            }
        }

        double part;

        if (!hasTriggeredBefore) {
            // проскок → суммируем все доли до текущего уровня
            part = 0;
            for (int j = 0; j <= index; j++) {
                part += CLOSE_PARTS[j];
            }
        } else {
            // обычный шаг
            part = CLOSE_PARTS[index];
        }

        BigDecimal targetClose = fullSize.multiply(BigDecimal.valueOf(part));
        BigDecimal alreadyClosed = fullSize.subtract(remaining.get());

        return targetClose.subtract(alreadyClosed);
    }

    private BigDecimal normalizeSize(BigDecimal size, BigDecimal lotSz) {
        if (lotSz == null) return size;

        return size.divide(lotSz, 0, RoundingMode.DOWN).multiply(lotSz);
    }

    private BigDecimal safePart(BigDecimal requested) {
        BigDecimal current = remaining.get();
        return requested.min(current);
    }
}
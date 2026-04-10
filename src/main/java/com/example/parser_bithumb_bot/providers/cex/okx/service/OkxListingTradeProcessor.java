package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.app.core.ActualPrice;
import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.app.service.TradeExecutor;
import com.example.parser_bithumb_bot.providers.cex.okx.utils.OkxParserUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OkxListingTradeProcessor implements TradeExecutor {

    private final OkxOrderRequestService requestService;
    private final OkxWsPriceService wsPriceService;
    private final OkxCalculation okxCalculation;
    private final ActualPrice actualPrice;

    // ✅ защита от дублей
    private final Set<String> activeTrades = ConcurrentHashMap.newKeySet();

    @Override
    public Mono<Void> execute(ListingEvent event) {

        String symbol = event.getShortName();
        if (symbol == null || symbol.isBlank()) {
            return Mono.empty();
        }

        String instId = symbol + "-USDT-SWAP";

        // ✅ анти-дубль
        if (!activeTrades.add(instId)) {
            System.out.println("ALREADY RUNNING: " + instId);
            return Mono.empty();
        }

        Double price = actualPrice.getPrice(symbol);
        if (price == null || price <= 0) {
            activeTrades.remove(instId);
            return Mono.empty();
        }

        BigDecimal usdtAmount = BigDecimal.valueOf(TradingConfig.AMOUNT_MONEY_TO_BUY);
        BigDecimal priceBd = BigDecimal.valueOf(price);

        return requestService.getInstrumentInfo(instId)
                .map(okxCalculation::extractInstrument)

                .flatMap(inst -> {

                    int leverage = okxCalculation.normalizeLeverage(
                            TradingConfig.LEVERAGE, inst);

                    BigDecimal sz = okxCalculation.calculateSz(
                            usdtAmount, priceBd, inst.getCtVal(), leverage);

                    sz = okxCalculation.normalizeSz(sz, inst);

                    if (sz.compareTo(BigDecimal.ZERO) <= 0) {
                        System.out.println("SIZE = 0 → SKIP");
                        activeTrades.remove(instId);
                        return Mono.empty();
                    }

                    BigDecimal finalSz = sz;
                    return requestService.setLeverage(instId, leverage)

                            .then(requestService.placeMarketOrder(instId, sz.toPlainString()))

                            .map(resp -> OkxParserUtils.extractTargetValue(resp, "ordId"))

                            .doOnNext(orderId ->
                                    System.out.println("ORDER CREATED: " + orderId)
                            )

                            // ✅ ЗАПУСК WS ОТДЕЛЬНО (НЕ БЛОКИРУЕТ)
                            .doOnSuccess(orderId -> {
                                wsPriceService.runLongSniper(instId, finalSz, leverage, inst)
                                        .doFinally(s -> {
                                            System.out.println("WS FINISHED: " + instId);
                                            activeTrades.remove(instId);
                                        })
                                        .subscribe();
                            })

                            .then();
                })

                .doOnError(e -> {
                    System.out.println("EXECUTE ERROR: " + e.getMessage());
                    activeTrades.remove(instId);
                });
    }
}
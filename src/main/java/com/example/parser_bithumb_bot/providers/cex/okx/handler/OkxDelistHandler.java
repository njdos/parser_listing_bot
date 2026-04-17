package com.example.parser_bithumb_bot.providers.cex.okx.handler;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.app.core.ActualPrice;
import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.providers.cex.okx.service.DeListingService;
import com.example.parser_bithumb_bot.providers.cex.okx.service.PrepareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class OkxDelistHandler implements OkxTradeHandler {

    private final DeListingService deListingService;
    private final PrepareService prepareService;
    private final ActualPrice actualPrice;

    private final Set<String> activeTrades = ConcurrentHashMap.newKeySet();

    @Override
    public boolean supports(ListingEvent.ActionType action) {
        return action == ListingEvent.ActionType.BINANCE_DELIST;
    }

    @Override
    public Mono<Void> handle(ListingEvent event) {
        String symbol = event.getShortName();
        if (symbol == null || symbol.isBlank()) {
            log.info("Empty symbol: {}", symbol);
            return Mono.empty();
        }

        String instId = symbol + "-USDT-SWAP";
        Double price = actualPrice.getPrice(symbol);
        if (price == null || price <= 0) {
            activeTrades.remove(instId);
            log.info("ABSENT PRICE: {}", instId);
            return Mono.empty();
        }

        BigDecimal usdtAmount = BigDecimal.valueOf(TradingConfig.AMOUNT_MONEY_TO_BUY);
        BigDecimal priceBd = BigDecimal.valueOf(price);

        return prepareService.listing(instId, priceBd, usdtAmount)
                .flatMap(deListingService::execute)
                .doOnError(e -> {
                    activeTrades.remove(instId);
                    log.info("EXECUTE ERROR: {}", e.getMessage());
                });
    }
}
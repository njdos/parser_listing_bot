package com.example.parser_bithumb_bot.providers.cex.okx.handler;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.app.core.ActualPrice;
import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.providers.cex.okx.http.OkxOrderRequestService;
import com.example.parser_bithumb_bot.providers.cex.okx.service.ListingService;
import com.example.parser_bithumb_bot.providers.cex.okx.service.OkxCalculation;
import com.example.parser_bithumb_bot.providers.cex.okx.service.PrepareService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class OkxListingHandler implements OkxTradeHandler {

    private final OkxOrderRequestService requestService;
    private final ListingService listingService;
    private final PrepareService prepareService;
    private final OkxCalculation okxCalculation;
    private final ActualPrice actualPrice;

    private final Set<String> activeTrades = ConcurrentHashMap.newKeySet();

    @Override
    public boolean supports(ListingEvent.ActionType action) {
        return action == ListingEvent.ActionType.LISTING;
    }

    @Override
    public Mono<Void> handle(ListingEvent event) {
        String symbol = event.getShortName();
        if (symbol == null || symbol.isBlank()) {
            return Mono.empty();
        }

        String instId = symbol + "-USDT-SWAP";
        if (!activeTrades.add(instId)) {
            System.out.println("ALREADY RUNNING: " + instId);
            return Mono.empty();
        }

        Double price = actualPrice.getPrice(symbol);
        if (price == null || price <= 0) {
            activeTrades.remove(instId);
            System.out.println("ABSENT PRICE: " + instId);
            return Mono.empty();
        }

        BigDecimal usdtAmount = BigDecimal.valueOf(TradingConfig.AMOUNT_MONEY_TO_BUY);
        BigDecimal priceBd = BigDecimal.valueOf(price);

        return prepareService.listing(instId, priceBd, usdtAmount)
                .flatMap(listingService::execute)
                .doOnError(e -> {
                    activeTrades.remove(instId);
                    System.out.println("EXECUTE ERROR: " + e.getMessage());
                });
    }
}

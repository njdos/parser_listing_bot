package com.example.parser_bithumb_bot.providers.cex.bingx.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.app.core.ActualPrice;
import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.app.service.TradeExecutor;
import com.example.parser_bithumb_bot.providers.cex.bingx.dto.BingXOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class BinxListingTradeProcessor implements TradeExecutor {

    private final BingxOrderService orderService;
    private final ActualPrice actualPrice;

    @Override
    public Mono<Void> execute(ListingEvent event) {
        return Mono.empty();
//        String symbol = event.getShortName();
//        if (symbol == null || symbol.isBlank()) {
//        }
//
//        Double price = actualPrice.getPrice(symbol);
//        if (price == null || price <= 0) {
//            return Mono.empty();
//        }
//
//        BigDecimal priceBD = BigDecimal.valueOf(price);
//        BigDecimal amount = BigDecimal.valueOf(TradingConfig.AMOUNT_MONEY_TO_BUY);
//        BigDecimal qty = amount.divide(priceBD, 8, RoundingMode.DOWN)
//                .setScale(4, RoundingMode.DOWN);
//
//        int leverage = TradingConfig.LEVERAGE;
//
//        BingXOrderRequest order = BingXOrderRequest.builder()
//                .symbol(symbol + "-USDT")
//                .positionSide("LONG")
//                .type("MARKET")
//                .side("BUY")
//                .quantity(qty.toPlainString())
//                .build();
//
//        return orderService.setLeverage(symbol + "-USDT", leverage, order.getPositionSide())
//                .then(orderService.placeOrder(order))
//                .then();
    }

}
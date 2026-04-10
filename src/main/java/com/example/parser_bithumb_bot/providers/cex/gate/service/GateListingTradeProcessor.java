package com.example.parser_bithumb_bot.providers.cex.gate.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.app.core.ActualPrice;
import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.app.service.TradeExecutor;
import com.example.parser_bithumb_bot.providers.cex.gate.dto.GateOrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class GateListingTradeProcessor implements TradeExecutor {

    private final GateOrderService orderService;
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
//        String contract = symbol + "_USDT";
//
//        double amount = TradingConfig.AMOUNT_MONEY_TO_BUY;
//        int leverage = TradingConfig.LEVERAGE;
//
//        int size = Math.max(1, (int) (amount / price));
//
//        GateOrderRequest order = GateOrderRequest.builder()
//                .contract(contract)
//                .size(size)
//                .price("0")
//                .tif("ioc")
//                .build();
//
//        return orderService.setLeverage(contract, leverage)
//                .then(orderService.placeOrder(order))
//                .then();
    }
}
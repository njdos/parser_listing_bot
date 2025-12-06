package com.example.parser_bithumb_bot.providers.gate;

import com.example.parser_bithumb_bot.providers.gate.dto.GateOrderRequest;
import com.example.parser_bithumb_bot.providers.gate.service.GateMarketService;
import com.example.parser_bithumb_bot.providers.gate.service.GateOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;

@Component
public class GateAPI {

    @Autowired
    private GateOrderService orderService;

    @Autowired
    private GateMarketService marketService;

    private static final double USDT_AMOUNT = 2.5;
    private static final int LEVERAGE = 5;

    public void placeLong(Set<String> newPairs) {

        Map<String, Double> entryPrices = marketService.getPrice(newPairs);

        for (String token : newPairs) {

            Double entryPrice = entryPrices.get(token);
            if (entryPrice == null || entryPrice <= 0) {
                System.out.println("Не удалось получить цену для " + token);
                continue;
            }

            String contract = token + "_USDT";

            try {
                orderService.setLeverage(contract, LEVERAGE);

                GateOrderRequest orderRequest = new GateOrderRequest();
                orderRequest.setContract(contract);
                orderRequest.setSide("sell");
                orderRequest.setSize(String.valueOf(3));
                orderRequest.setPrice("0");
                orderRequest.setTif("ioc");
                orderRequest.setText("t-long_" + token + "_usdt");

                String response = orderService.placeOrder(orderRequest);
                System.out.println("Ордер для " + token + " отправлен. Ответ: " + response);

            } catch (Exception e) {
                System.err.println("Ошибка при создании ордера для " + token);
                e.printStackTrace();
            }
        }
    }
}

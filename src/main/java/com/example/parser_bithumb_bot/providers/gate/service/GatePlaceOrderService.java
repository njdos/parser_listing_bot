package com.example.parser_bithumb_bot.providers.gate.service;

import com.example.parser_bithumb_bot.providers.gate.dto.GateContractInfo;
import com.example.parser_bithumb_bot.providers.gate.dto.GateMarketInfo;
import com.example.parser_bithumb_bot.providers.gate.dto.GateOrderRequest;
import com.example.parser_bithumb_bot.providers.telegram.TelegramAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class GatePlaceOrderService {

    @Autowired
    private GateOrderService orderService;

    @Autowired
    private GateMarketService marketService;

    @Autowired
    private TelegramAPI telegramAPI;

    @Value("${gate.leverage:}")
    private String GATE_LEVERAGE;
    @Value("${gate.order-usdt:}")
    private String GATE_ORDER_USDT;
    @Value("${gate.tp-percent:}")
    private String GATE_TP_PERCENT;
    @Value("${gate.sl-percent:}")
    private String GATE_SL_PERCENT;

    public void placePosition(Set<String> tokens, boolean isLong) {

        Map<String, GateMarketInfo> marketInfo = marketService.getMarketInfo();
        Map<String, GateContractInfo> contractsInfo = marketService.getContractsInfo();

        for (String token : tokens) {
            try {

                GateMarketInfo mi = marketInfo.get(token.toUpperCase());
                GateContractInfo ci = contractsInfo.get(token.toUpperCase());

                if (mi == null || ci == null) {
                    System.out.println("Missing info for: " + token);
                    telegramAPI.sendTelegramMessage(null, "Missing info for: " + token);
                    continue;
                }

                int leverage = Integer.parseInt(GATE_LEVERAGE);
                double orderUsdt = Double.parseDouble(GATE_ORDER_USDT);
                double tpPercent = Double.parseDouble(GATE_TP_PERCENT);
                double slPercent = Double.parseDouble(GATE_SL_PERCENT);

                ResponseEntity<String> respLeverage = orderService.setLeverage(ci.getName(), leverage);
                if (respLeverage.getStatusCode().is2xxSuccessful()) {
                    telegramAPI.sendTelegramMessage(null,
                            "🔔 LEVERAGE UPDATED\n" +
                                    "Token: " + token + "\n" +
                                    "New leverage: " + leverage);
                } else {
                    telegramAPI.sendTelegramMessage(null,
                            "❌ ERROR updating leverage for " + token +
                                    "\nResponse: " + respLeverage.getBody());
                }

                long size = orderService.calculateContracts(
                        orderUsdt,
                        leverage,
                        mi.getMarkPrice(),
                        mi.getQuantoMultiplier(),
                        ci.getOrderSizeStep(),
                        ci.getOrderSizeMin()
                );
                if (size <= 0) {
                    System.out.println("Size too small for " + token);
                    telegramAPI.sendTelegramMessage(null, "Size too small for " + token);
                    continue;
                }

                GateOrderRequest req = new GateOrderRequest();
                req.setContract(ci.getName());
                req.setSide(isLong ? "buy" : "sell");
                req.setSize(String.valueOf(isLong ? size : -size));
                req.setPrice("0");
                req.setTif("ioc");
                req.setText("t-" + token + (isLong ? "-long" : "-short"));

                ResponseEntity<String> respOrder = orderService.placeOrder(req);
                if (respOrder.getStatusCode().is2xxSuccessful()) {
                    telegramAPI.sendTelegramMessage(null,
                            "🔔 ORDER FILLED\n" +
                                    "Token: " + token + "\n" +
                                    "Side: " + (isLong ? "LONG" : "SHORT") + "\n" +
                                    "Size: " + size + "\n" +
                                    "Entry: " + mi.getMarkPrice());
                } else {
                    telegramAPI.sendTelegramMessage(null,
                            "❌ ERROR placing order for " + token +
                                    "\nResponse: " + respOrder.getBody());
                }

//                double tpPrice = isLong
//                        ? mi.getMarkPrice() * (1 + tpPercent)
//                        : mi.getMarkPrice() * (1 - tpPercent);
//
//                double slPrice = isLong
//                        ? mi.getMarkPrice() * (1 - slPercent)
//                        : mi.getMarkPrice() * (1 + slPercent);
//
//                String side = isLong ? "sell" : "buy";
//                if (tpPrice > 0) {
//                    ResponseEntity<String> respTp = orderService.sendTrigger(ci.getName(), side, size, tpPrice);
//                    if (respTp.getStatusCode().is2xxSuccessful()) {
//                        telegramAPI.sendTelegramMessage(null,
//                                "🎯 TP SET for " + token + " at " + tpPrice);
//                    } else {
//                        telegramAPI.sendTelegramMessage(null,
//                                "❌ ERROR setting TP for " + token +
//                                        "\nResponse: " + respTp.getBody());
//                    }
//                }
//
//
//                if (slPrice > 0) {
//                    ResponseEntity<String> respSl = orderService.sendTrigger(ci.getName(), side, size, slPrice);
//                    if (respSl.getStatusCode().is2xxSuccessful()) {
//                        telegramAPI.sendTelegramMessage(null,
//                                "🛑 SL SET for " + token + " at " + slPrice);
//                    } else {
//                        telegramAPI.sendTelegramMessage(null,
//                                "❌ ERROR setting SL for " + token +
//                                        "\nResponse: " + respSl.getBody());
//                    }
//                }

            } catch (Exception e) {
                System.err.println("Order failed for " + token);
                e.printStackTrace();
            }
        }
    }
}

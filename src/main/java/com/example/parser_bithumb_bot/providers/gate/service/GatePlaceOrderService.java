package com.example.parser_bithumb_bot.providers.gate.service;

import com.example.parser_bithumb_bot.providers.gate.dto.GateContractInfo;
import com.example.parser_bithumb_bot.providers.gate.dto.GateMarketInfo;
import com.example.parser_bithumb_bot.providers.gate.dto.GateOrderRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class GatePlaceOrderService {

    @Autowired
    private GateOrderService orderService;

    @Autowired
    private GateMarketService marketService;

    @Value("${gate.leverage:}")
    private String GATE_LEVERAGE;
    @Value("${gate.order-usdt:}")
    private String GATE_ORDER_USDT;
    @Value("${gate.tp-percent:}")
    private String GATE_TP_PERCENT;
    @Value("${gate.sl-percent:}")
    private String GATE_SL_PERCENT;

    public void placePosition(Set<String> tokens, boolean isLong) {

        Map<String, GateMarketInfo> marketInfo = marketService.getMarketInfo(tokens);
        Map<String, GateContractInfo> contractsInfo = marketService.getContractsInfo();

        for (String token : tokens) {
            try {

                GateMarketInfo mi = marketInfo.get(token.toUpperCase());
                GateContractInfo ci = contractsInfo.get(token.toUpperCase());

                if (mi == null || ci == null) {
                    System.out.println("Missing info for: " + token);
                    continue;
                }

                int leverage = Integer.parseInt(GATE_LEVERAGE);
                double orderUsdt = Double.parseDouble(GATE_ORDER_USDT);
                double tpPercent = Double.parseDouble(GATE_TP_PERCENT);
                double slPercent = Double.parseDouble(GATE_SL_PERCENT);

                orderService.setLeverage(ci.getName(), leverage);

                Double entry = mi.getMarkPrice();
                long size = orderService.calculateContracts(
                        orderUsdt,
                        leverage,
                        entry,
                        mi.getQuantoMultiplier(),
                        ci.getOrderSizeStep(),
                        ci.getOrderSizeMin()
                );

                if (size <= 0) {
                    System.out.println("Size too small for " + token);
                    continue;
                }

                double tpFactor = tpPercent / 100.0;
                double slFactor = slPercent / 100.0;

                double tp, sl;

                if (isLong) {
                    tp = entry * (1 + tpFactor);
                    sl = entry * (1 - slFactor);
                } else {
                    tp = entry * (1 - tpFactor);
                    sl = entry * (1 + slFactor);
                }

                GateOrderRequest req = new GateOrderRequest();
                req.setContract(ci.getName());
                req.setSide(isLong ? "buy" : "sell");
                req.setSize(String.valueOf(size));
                req.setPrice("0");
                req.setTif("ioc");
                req.setText("t-auto_" + (isLong ? "long" : "short") + "_" + token);

                req.setTake_profit_price(String.valueOf(tp));
                req.setStop_loss_price(String.valueOf(sl));

                String resp = orderService.placeOrder(req);
                System.out.println("ORDER OK " + token + ": " + resp);

            } catch (Exception e) {
                System.err.println("Order failed for " + token);
                e.printStackTrace();
            }
        }
    }
}

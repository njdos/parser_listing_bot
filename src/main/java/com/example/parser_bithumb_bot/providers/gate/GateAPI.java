package com.example.parser_bithumb_bot.providers.gate;

import com.example.parser_bithumb_bot.providers.gate.service.GatePlaceOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GateAPI {

    @Autowired
    private GatePlaceOrderService gatePlaceOrderService;

    public void placePosition(Set<String> tokens, boolean isLong) {
        gatePlaceOrderService.placePosition(tokens, isLong);
    }
}

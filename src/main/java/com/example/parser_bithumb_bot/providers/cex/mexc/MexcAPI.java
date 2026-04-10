package com.example.parser_bithumb_bot.providers.cex.mexc;

import com.example.parser_bithumb_bot.providers.cex.mexc.service.MexcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class MexcAPI {

    @Autowired
    private MexcService mexcService;

    public Mono<Map<String, Double>> getAllPrices() {
        return mexcService.getAllPrices();
    }

}

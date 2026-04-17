package com.example.parser_bithumb_bot.providers.cex.okx.service;

import com.example.parser_bithumb_bot.app.config.TradingConfig;
import com.example.parser_bithumb_bot.providers.cex.okx.dto.ListingContext;
import com.example.parser_bithumb_bot.providers.cex.okx.http.OkxOrderRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrepareService {

    private final OkxOrderRequestService requestService;
    private final OkxCalculation calculation;


    public Mono<ListingContext> listing(String instId,
                                        BigDecimal price,
                                        BigDecimal usdtAmount) {

        return requestService.getInstrumentInfo(instId)
                .map(calculation::extractInstrument)
                .flatMap(inst -> {

                    int leverage = calculation.normalizeLeverage(
                            TradingConfig.LEVERAGE, inst);

                    BigDecimal sz = calculation.calculateSz(
                            usdtAmount, price, inst.getCtVal(), leverage);

                    sz = calculation.normalizeSz(sz, inst);

                    if (sz.compareTo(BigDecimal.ZERO) <= 0) {
                        return Mono.empty();
                    }

                    return Mono.just(new ListingContext(instId, sz, leverage, inst));
                })
                .onErrorResume(e -> {
                    log.warn("DISPATCH ERROR (suppressed): {}", e.getMessage());
                    return Mono.empty();
                });
    }

}

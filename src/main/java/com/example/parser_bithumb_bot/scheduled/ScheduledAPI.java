package com.example.parser_bithumb_bot.scheduled;

import com.example.parser_bithumb_bot.app.core.ActualPrice;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ScheduledAPI {

    private final ActualPrice actualPrice;

    @Scheduled(fixedRate = 60_000)
    public void updatePrices() {
        actualPrice.updatePrices().subscribe();
    }
}

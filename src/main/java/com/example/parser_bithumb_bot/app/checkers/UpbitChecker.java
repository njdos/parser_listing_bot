package com.example.parser_bithumb_bot.app.checkers;

import com.example.parser_bithumb_bot.app.core.BasePairChecker;
import com.example.parser_bithumb_bot.providers.telegram.TelegramAPI;
import com.example.parser_bithumb_bot.providers.upbit.UpbitAPI;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class UpbitChecker extends BasePairChecker {

    @Autowired
    private UpbitAPI upbitAPI;

    @Autowired
    private TelegramAPI telegramAPI;

    @PostConstruct
    public void initChecker() {
        init();
    }

    @Override
    protected Set<String> fetchPairs() throws Exception {
        return upbitAPI.fetchPairs();
    }

    @Override
    protected String getExchangeName() {
        return "Upbit";
    }

    @Override
    protected void sendMessage(String msg) {
        telegramAPI.sendTelegramMessage(msg);
    }
}

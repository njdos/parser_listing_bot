package com.example.parser_bithumb_bot.app.checkers;

import com.example.parser_bithumb_bot.app.core.BasePairChecker;
import com.example.parser_bithumb_bot.providers.bithumb.BithumbAPI;
import com.example.parser_bithumb_bot.providers.telegram.TelegramAPI;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class BithumbChecker extends BasePairChecker {

    @Autowired
    private BithumbAPI bithumbAPI;

    @Autowired
    private TelegramAPI telegramAPI;

    @PostConstruct
    public void initChecker() {
        init();
    }

    @Override
    protected Set<String> fetchPairs() throws Exception {
        return bithumbAPI.fetchPairs();
    }

    @Override
    protected String getExchangeName() {
        return "Bithumb";
    }

    @Override
    protected void sendMessage(String msg) {
        telegramAPI.sendTelegramMessage(null, msg);
    }
}

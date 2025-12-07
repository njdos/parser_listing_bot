package com.example.parser_bithumb_bot.providers.telegram;

import com.example.parser_bithumb_bot.providers.telegram.service.TelegramService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TelegramAPI {

    @Autowired
    private TelegramService telegramService;

    public void sendTelegramMessage(String text) {
        telegramService.sendMessage(text);
    }
}

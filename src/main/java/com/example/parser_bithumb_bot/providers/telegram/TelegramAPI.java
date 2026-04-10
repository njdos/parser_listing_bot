package com.example.parser_bithumb_bot.providers.telegram;

import com.example.parser_bithumb_bot.providers.telegram.service.Sender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TelegramAPI {

    @Autowired
    private Sender sender;

    public void sendTelegramMessage(String text) {
        sender.sendMessage(text);
    }
}

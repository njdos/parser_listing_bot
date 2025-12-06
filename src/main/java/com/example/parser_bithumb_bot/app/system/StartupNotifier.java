package com.example.parser_bithumb_bot.app.system;

import com.example.parser_bithumb_bot.providers.telegram.TelegramAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupNotifier {

    @Autowired
    private TelegramAPI telegramAPI;

    @EventListener(ApplicationReadyEvent.class)
    public void sendStartupMessage() {
        String message = "Програма успішно запущена!";
        telegramAPI.sendTelegramMessage(null, message);
    }
}

package com.example.parser_bithumb_bot.app.system;

import com.example.parser_bithumb_bot.providers.telegram.TelegramAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class StartupNotifier {

    @Autowired
    private TelegramAPI telegramAPI;

    @Value("${app.mode:}")
    private String appMode;

    @EventListener(ApplicationReadyEvent.class)
    public void sendStartupMessage() {
        if ("prod".equals(appMode)) {
            telegramAPI.sendTelegramMessage("Application Successfully Started!");
        } else {
            System.out.println("Application Successfully Started! in " + appMode + " mode");
        }
    }
}

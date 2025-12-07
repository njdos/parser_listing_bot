package com.example.parser_bithumb_bot.providers.telegram.service;

import com.example.parser_bithumb_bot.app.core.BasePairChecker;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

@Service
public class TelegramService extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        handleUpdate(update);
    }

    @Override
    public String getBotUsername() {
        return "Crypto Listing Parser Bot";
    }

    @Override
    public String getBotToken() {
        return "8156206670:AAGWbZw3kK1FPbs_GzCNODdMbow3ECSKo8Y";
    }

    public void sendMessage(String text) {
        SendMessage msg = SendMessage.builder()
                .chatId("-1003304663557")
                .messageThreadId(6)
                .text(text)
                .parseMode("Markdown")
                .build();
        try {
            execute(msg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void handleUpdate(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {

            String message = update.getMessage().getText();

            if (message.equalsIgnoreCase("/info")) {
                Map<String, String> lastLog = BasePairChecker.getLastLog();
                StringBuilder sb = new StringBuilder();

                for (Map.Entry<String, String> entry : lastLog.entrySet()) {
                    sb.append(entry.getValue()).append("\n");
                }

                sendMessage(sb.toString());
                return;
            }

            sendMessage("You said: " + message);
        }
    }
}

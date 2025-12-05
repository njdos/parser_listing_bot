package com.example.parser_bithumb_bot.providers.telegram.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class TelegramService extends TelegramLongPollingBot {

    @Override
    public void onUpdateReceived(Update update) {
        handleUpdate(update);
    }

    @Override
    public String getBotUsername() {
        return "FreeFireBithumb";
    }

    @Override
    public String getBotToken() {
        return "8474781102:AAGa4ZvSPv8Vh8QU6vVczxbX0Lp5vVAjAeU";
    }

    public void sendMessage(Long chatId, String text) {
        SendMessage msg = SendMessage.builder()
                .chatId(chatId != null ? chatId.toString() : "971483502")
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
            long chatId = update.getMessage().getChatId();
            String message = update.getMessage().getText();

            sendMessage(chatId, "You said: " + message);
        }
    }
}

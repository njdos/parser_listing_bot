package com.example.parser_bithumb_bot.providers.telegram.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.Map;

@Service
public class Sender extends TelegramLongPollingBot {

    @Value("${telegram.chat.id:}")
    private Long CHAT_ID;

    @Value("${telegram.message.thread.id:}")
    private Integer MESSAGE_THREAD_ID;

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
                .chatId(CHAT_ID.toString())
                .messageThreadId(MESSAGE_THREAD_ID)
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
        Message updMess = update.getMessage();

        if (!updMess.getChatId().equals(CHAT_ID)
                || !updMess.getMessageThreadId().equals(MESSAGE_THREAD_ID)) {
            return;
        }

        if (update.hasMessage() && updMess.hasText()) {
            String message = updMess.getText();

            if (message.equalsIgnoreCase("/info")) {
                return;
            }

            sendMessage("You said: " + message);
        }
    }
}

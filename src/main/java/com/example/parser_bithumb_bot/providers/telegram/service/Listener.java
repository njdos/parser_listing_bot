package com.example.parser_bithumb_bot.providers.telegram.service;

import com.example.parser_bithumb_bot.providers.telegram.model.ListingEvent;
import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.regex.Pattern;

@Service
public class Listener {

    private SimpleTelegramClient client;

    @PostConstruct
    public void init() throws Exception {
        Init.init();
        try (SimpleTelegramClientFactory factory = new SimpleTelegramClientFactory()) {
            var apiToken = new APIToken(35054448, "685f7150c6d811ca11bbd08bedef0086");
            TDLibSettings settings = TDLibSettings.create(apiToken);
            settings.setUseTestDatacenter(false);

            Path sessionPath = Paths.get("tdlight-session");
            settings.setDatabaseDirectoryPath(sessionPath.resolve("data"));
            settings.setDownloadedFilesDirectoryPath(sessionPath.resolve("downloads"));

            SimpleTelegramClientBuilder builder = factory.builder(settings);
            var auth = AuthenticationSupplier.consoleLogin();

            builder.addUpdateHandler(TdApi.UpdateAuthorizationState.class, this::onUpdateAuthorizationState);
            builder.addUpdateHandler(TdApi.UpdateNewMessage.class, this::onUpdateNewMessage);

            this.client = builder.build(auth);

            System.out.println("🚀 Telegram client started");
        }
    }

    private void onUpdateAuthorizationState(TdApi.UpdateAuthorizationState update) {
        var state = update.authorizationState;
        System.out.println("onUpdateAuthorizationState: " + state);
        if (state instanceof TdApi.AuthorizationStateWaitPhoneNumber) {
            System.out.println("Waiting for phone...");
        } else if (state instanceof TdApi.AuthorizationStateWaitCode) {
            System.out.print("Enter code: ");
            try {
                String code = new Scanner(System.in).nextLine();
                client.send(new TdApi.CheckAuthenticationCode(code), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            System.out.print("Enter 2FA password: ");
            String password = new Scanner(System.in).nextLine();
            client.send(new TdApi.CheckAuthenticationPassword(password), null);
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            System.out.println("Logged in");
        } else if (state instanceof TdApi.AuthorizationStateLoggingOut) {
            System.out.println("Logging out...");
        } else if (state instanceof TdApi.AuthorizationStateClosing) {
            System.out.println("Closing...");
        } else if (state instanceof TdApi.AuthorizationStateClosed) {
            System.out.println("Closed");
        }
    }

    private void onUpdateNewMessage(TdApi.UpdateNewMessage update) {
        TdApi.Message msg = update.message;

        if (!(msg.content instanceof TdApi.MessageText textMsg)) {
            return;
        }

        parseMessage(msg.chatId, textMsg.text.text)
                .filter(events -> !events.isEmpty())
                .ifPresentOrElse(events -> events.stream().forEach(event -> {
                    System.out.println("🚀 EVENT:");
                    System.out.println("Token: " + event.getShortName());
                    System.out.println("Source: " + event.getSource());
                    System.out.println("Action: " + event.getAction());
                    System.out.println("----");
                }), () -> {
                    System.out.println("🚀 Some other test:");
                    System.out.println(msg.chatId);
                    System.out.println(textMsg.text.text);
                });
    }

    private Optional<List<ListingEvent>> parseMessage(long chatId, String text) {
        if (chatId == -1001124574831L) {
            return Optional.of(List.of(parseCoinListing(text)));
        } else if (chatId == -1001772001929L) {
            return parseRicklerAlerts(text);
        } else return Optional.empty();
    }

    private ListingEvent parseCoinListing(String text) {
        ListingEvent event = new ListingEvent();
        event.setRawText(text);
        var matcher = Pattern.compile("\\$(\\w+)").matcher(text);
        if (matcher.find()) {
            event.setShortName(matcher.group(1));
        }
        var sourceMatcher = Pattern.compile("\\[(.*?)\\]").matcher(text);
        if (sourceMatcher.find()) {
            event.setSource(sourceMatcher.group(1));
        }
        if (text.toLowerCase().contains("listed")) {
            event.setAction("listed");
        } else if (text.toLowerCase().contains("added")) {
            event.setAction("added");
        } else if (text.toLowerCase().contains("announced")) {
            event.setAction("announced");
        }
        return event;
    }

    private Optional<List<ListingEvent>> parseRicklerAlerts(String text) {
        List<ListingEvent> events = new ArrayList<>();
        var matcher = Pattern.compile("\\$(\\w+)").matcher(text);
        while (matcher.find()) {
            ListingEvent event = new ListingEvent();
            String token = matcher.group(1);
            event.setShortName(token);
            event.setRawText(text);
            var sourceMatcher = Pattern.compile("([A-Za-z]+) lists").matcher(text);
            if (sourceMatcher.find()) {
                event.setSource(sourceMatcher.group(1));
            }
            event.setAction("listed");
            events.add(event);
        }
        return events.isEmpty() ? Optional.empty() : Optional.of(events);
    }
}
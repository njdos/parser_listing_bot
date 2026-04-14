package com.example.parser_bithumb_bot.providers.telegram.service;

import com.example.parser_bithumb_bot.app.core.TradeDispatcher;
import com.example.parser_bithumb_bot.app.model.ListingEvent;
import com.example.parser_bithumb_bot.providers.telegram.ListingParser;
import it.tdlight.Init;
import it.tdlight.client.*;
import it.tdlight.jni.TdApi;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

@Service
@RequiredArgsConstructor
public class Listener {

    private final TradeDispatcher dispatcher;

    private SimpleTelegramClient client;
    private SimpleTelegramClientFactory factory;

    @PostConstruct
    public void init() throws Exception {
        Init.init();

        this.factory = new SimpleTelegramClientFactory();

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

    @PreDestroy
    public void destroy() {
        if (factory != null) {
            factory.close();
        }
        System.out.println("🛑 Telegram client stopped");
    }

    private void onUpdateAuthorizationState(TdApi.UpdateAuthorizationState update) {
        var state = update.authorizationState;

        if (state instanceof TdApi.AuthorizationStateWaitCode) {
            System.out.print("Enter code: ");
            String code = new Scanner(System.in).nextLine();
            client.send(new TdApi.CheckAuthenticationCode(code), null);
        } else if (state instanceof TdApi.AuthorizationStateWaitPassword) {
            System.out.print("Enter 2FA password: ");
            String password = new Scanner(System.in).nextLine();
            client.send(new TdApi.CheckAuthenticationPassword(password), null);
        } else if (state instanceof TdApi.AuthorizationStateReady) {
            System.out.println("✅ Logged in");
        }
    }

    private void onUpdateNewMessage(TdApi.UpdateNewMessage update) {
        TdApi.Message msg = update.message;
        if (!(msg.content instanceof TdApi.MessageText textMsg)) {
            return;
        }

        List<ListingEvent> events = ListingParser.parse(textMsg.text.text);
        if (events.isEmpty()) return;

        dispatcher.dispatch(events).subscribe();
    }

}
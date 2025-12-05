package com.example.parser_bithumb_bot.app.mapper.service;

import com.example.parser_bithumb_bot.providers.bithumb.service.BithumbService;
import com.example.parser_bithumb_bot.providers.telegram.TelegramAPI;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

@Component
public class CheckNewPairsService {

    @Autowired
    private BithumbService bithumbService;

    @Autowired
    private TelegramAPI telegramAPI;

    private static final long DEFAULT_CHAT_ID = 971483502;
    private static Set<String> knownPairs = new HashSet<>();

    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @PostConstruct
    public void init() {
        try {
            knownPairs = bithumbService.fetchPairs();
            System.out.println("Initial pairs loaded: " + knownPairs.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void start() {
        try {
            Set<String> current = bithumbService.fetchPairs();

            Set<String> newPairs = new HashSet<>();
            for (String pair : current) {
                if (!knownPairs.contains(pair)) {
                    newPairs.add(pair);
                }
            }

            String currentTime = LocalDateTime.now().format(dtf);
            StringBuilder msg = new StringBuilder();

            if (!newPairs.isEmpty()) {
                msg.append("\uD83D\uDD25 New pairs added at ").append(currentTime).append(",\n");
                for (String p : newPairs) {
                    msg.append("- ").append(p).append("\n");
                }
                telegramAPI.sendTelegramMessage(DEFAULT_CHAT_ID, msg.toString());
            } else {
                msg.append("\uD83D\uDEAB Checked at ").append(currentTime)
                        .append(", total pairs = ").append(current.size())
                        .append(", no new pairs.");
            }

            System.out.println(msg);

            knownPairs = current;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

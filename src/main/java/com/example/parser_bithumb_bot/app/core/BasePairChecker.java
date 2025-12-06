package com.example.parser_bithumb_bot.app.core;

import com.example.parser_bithumb_bot.providers.gate.GateAPI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
public abstract class BasePairChecker {

    @Autowired
    private GateAPI gateAPI;

    private static final HashMap<String, String> lastLog = new HashMap<>();

    protected static final DateTimeFormatter dtf =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected Set<String> knownPairs = new HashSet<>();

    protected abstract Set<String> fetchPairs() throws Exception;

    protected abstract String getExchangeName();

    protected abstract void sendMessage(String msg);

    public void init() {
        try {
            knownPairs = fetchPairs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void check() {
        try {
            Set<String> current = fetchPairs();

            Set<String> newPairs = new HashSet<>();
            for (String p : current) {
                if (!knownPairs.contains(p)) {
                    newPairs.add(p);
                }
            }

            String time = LocalDateTime.now().format(dtf);
            String mess = String.format("Checked %s — total: %s, new: %s,    Time: %s",
                    getExchangeName(), current.size(), newPairs.size(), time);

            if (!knownPairs.isEmpty() && !newPairs.isEmpty()) {
                StringBuilder msg = new StringBuilder();
                msg.append("🔥 NEW on ").append(getExchangeName())
                        .append(" at ").append(time).append("\n");

                for (String p : newPairs) {
                    msg.append("- ").append(p).append("\n");
                }

                msg.append("\n").append("\n").append(mess);

                sendMessage(msg.toString());
            }

            System.out.println(mess);

            knownPairs = current;
            lastLog.put(getExchangeName(), mess);
            if (!newPairs.isEmpty()) gateAPI.placePosition(newPairs, true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> getLastLog() {
        return lastLog;
    }
}

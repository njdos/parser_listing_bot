package com.example.parser_bithumb_bot.providers.bithumb.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class UpbitFastListingService {

    private static final String LIST_PAIRS_URI = "https://api.upbit.com/v1/market/all";
    private static final String ANNOUNCEMENTS_URI = "https://api-manager.upbit.com/api/v1/announcements?os=web&page=1&per_page=20&category=trade";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    private final Map<String, String> knownMarkets = new ConcurrentHashMap<>();

//    @PostConstruct
    public void start() {
        fetchAllPairs();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::pollAnnouncements, 0, 100, TimeUnit.MILLISECONDS);
    }

    // --- Fetch all existing markets ---
    private void fetchAllPairs() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(LIST_PAIRS_URI))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode arr = mapper.readTree(response.body());

            for (JsonNode node : arr) {
                if (node.get("english_name").asText().equals("Katana")) continue;
                knownMarkets.put(
                        node.get("market").asText().split("-")[1],
                        node.get("english_name").asText()
                );
            }

            System.out.println("✅ Initial markets loaded: " + knownMarkets.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pollAnnouncements() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ANNOUNCEMENTS_URI))
                    .header("accept-language", "en-US,en;q=0.9,ru;q=0.8,ru-RU;q=0.7")
                    .GET()
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(this::handleAnnouncementsResponse)
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void handleAnnouncementsResponse(HttpResponse<String> response) {
        try {
            JsonNode notices = mapper.readTree(response.body())
                    .path("data").path("notices");

            for (JsonNode node : notices) {
                String title = node.path("title").asText();

                if (title.startsWith("Market Support for")) {

                    Pattern pattern = Pattern.compile("for (.*?)\\((.*?)\\)");
                    Matcher matcher = pattern.matcher(title);


                    while (matcher.find()) {
                        String name = matcher.group(1);
                        String symbol = matcher.group(2);
                        checkAndNotify(name, symbol);
                    }
                }
            }

        } catch (Exception e) {
            System.out.println(response.body());
            e.printStackTrace();
        }
    }

    private void checkAndNotify(String name, String symbol) {
        if (!knownMarkets.containsKey(symbol)) {
            knownMarkets.put(symbol, name);
            System.out.println("🔥 NEW MARKET DETECTED FROM ANNOUNCEMENT: " + name + " --- " + symbol);
            // TODO: Telegram / WS / trading logic
            knownMarkets.remove(symbol);
        }
    }
}
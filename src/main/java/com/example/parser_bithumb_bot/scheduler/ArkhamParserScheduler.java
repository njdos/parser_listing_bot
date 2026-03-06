package com.example.parser_bithumb_bot.scheduler;

import com.example.parser_bithumb_bot.model.CoinInfo;
import com.example.parser_bithumb_bot.service.ArkhamParserService;
import com.example.parser_bithumb_bot.service.MexcService;
import jakarta.annotation.PostConstruct;
import org.openqa.selenium.WebDriver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ArkhamParserScheduler {

    private final MexcService mexcService;
    private final ArkhamParserService parserService;
    private final WebDriver driver;

    public ArkhamParserScheduler(MexcService mexcService, ArkhamParserService parserService, WebDriver driver) {
        this.mexcService = mexcService;
        this.parserService = parserService;
        this.driver = driver;
    }

    @PostConstruct
    public void parseCoins() throws InterruptedException {
        List<String> tokens = mexcService.getTokens();

        for (String token : tokens) {
            try {
                System.out.println("Token: " + token);

                CoinInfo info = parserService.getTopHolders(driver, token);

                if (info == null) continue;

                System.out.println("Coin: " + info.coinName() + " Network: " + info.network());
                info.topHolders().forEach(th ->
                        System.out.println("Address: " + th.address() + "  ----------  USD: " + th.usd() + "  ----------  %: " + th.percent())
                );

                parserService.addAlert(driver, info);

            } catch (Exception e) {
                System.out.println("Error parsing token " + token + ": " + e.getMessage());
            }
        }
    }
}
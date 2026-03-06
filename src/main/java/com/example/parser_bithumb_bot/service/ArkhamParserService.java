package com.example.parser_bithumb_bot.service;

import com.example.parser_bithumb_bot.model.CoinInfo;
import com.example.parser_bithumb_bot.model.TopHolder;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ArkhamParserService {

    private static final int TIMEOUT = 10;
    private final int TARGET_SLEEP = 5_000;

    private final double MIN_CAP = 10_000_000;
    private final double MAX_CAP = 10_000_000_000.0;


    public CoinInfo getTopHolders(WebDriver driver, String token) throws Exception {
        if (!searchToken(driver, token)) return null;
        if (!validateToken(driver, token)) return null;

        String coinName = driver.findElement(By.cssSelector(".TokenNameCard-module__IP_nTq__tokenNameHeader")).getText();
        String chain = driver.findElement(By.cssSelector(".TokenNameCard-module__IP_nTq__selectedChain img")).getAttribute("alt");
        JavascriptExecutor js = (JavascriptExecutor) driver;

        double limit = 3_000_000;

        List<Map<String, Object>> holdersData = (List<Map<String, Object>>) js.executeScript(
                """
                        const rows = document.querySelectorAll('div.TokenTopHolders-module__0lehJa__topHolderRowContainer');
                        const seen = new Set();
                        
                        return Array.from(rows).map(row => {
                            const usdEl = row.querySelector('span.TokenTopHolders-module__0lehJa__topHolderUSD');
                            const percEl = row.querySelector('span.TokenTopHolders-module__0lehJa__topHolderPercent');
                            const link = row.querySelector('a[href*="/explorer/address/"]');
                        
                            if(!usdEl || !percEl || !link) return null;
                        
                            let usdText = usdEl.innerText.replace('$','').trim();
                            let usd = 0;
                            if (usdText.endsWith('B')) {
                                usd = parseFloat(usdText.replace('B','')) * 1_000_000_000;
                            } else if (usdText.endsWith('M')) {
                                usd = parseFloat(usdText.replace('M','')) * 1_000_000;     
                            } else if (usdText.endsWith('K')) {
                                usd = parseFloat(usdText.replace('K','')) * 1_000;        
                            } else {
                                usd = parseFloat(usdText.replace(/,/g,''));         
                            }
                        
                            if(usd < arguments[0]) return null; 
                        
                            let percent = parseFloat(percEl.innerText.replace('%','').trim());
                            const address = link.getAttribute('href').split('/').pop();
                        
                            if(seen.has(address)) return null; 
                            seen.add(address);
                        
                            return { address: address, usd: usd, percent: percent };
                        }).filter(x => x !== null);
                        """,
                limit
        );

        List<TopHolder> topHolders = new ArrayList<>();
        for (Map<String, Object> h : holdersData) {
            String address = (String) h.get("address");
            double usd = ((Number) h.get("usd")).doubleValue();
            double percent = ((Number) h.get("percent")).doubleValue();
            double usdInMillions = Math.round((usd / 1_000_000) * 10) / 10.0;
            topHolders.add(new TopHolder(address, usdInMillions, percent));
        }

        return new CoinInfo(coinName, chain, topHolders);
    }

    private boolean validateToken(WebDriver driver, String token) {
        WebElement marketCapEl = driver.findElement(By.xpath(
                "//div[contains(@class,'Token-module__Yf_0Ha__marketDataCell')]" +
                        "[.//div[contains(text(),'market cap')]]//div[contains(@class,'Token-module__Yf_0Ha__marketDataValue')]"
        ));
        String marketCapText = marketCapEl.getText().replaceAll("[$,]", "").trim();
        double marketCap = Double.parseDouble(marketCapText);

        if (marketCap < MIN_CAP || marketCap > MAX_CAP) {
            System.out.println("Market cap не підходить для токена " + token + ": " + marketCap);
            return false;
        }

        return true;
    }

    public void addAlert(WebDriver driver, CoinInfo info) throws Exception {
        driver.get("https://intel.arkm.com/alerts");

        Thread.sleep(TARGET_SLEEP);

        String alertNameText = info.coinName() + " Top Whale Alert";

        if (alertExists(driver, alertNameText)) {
            System.out.println("⚠ Alert already exists: " + alertNameText);
            return;
        }

        driver.findElement(By.cssSelector(".AlertsHome-module__8qJDSG__createAlertButton")).click();

        WebElement alertName = driver.findElement(By.xpath("//input[@placeholder='Alert name']"));
        alertName.clear();
        alertName.sendKeys(alertNameText);

        List<WebElement> addressInputs = driver.findElements(By.xpath("//input[@placeholder='Add an entity/address']"));
        WebElement whoseTransfers = addressInputs.getFirst();
        for (TopHolder holder : info.topHolders()) {
            whoseTransfers.sendKeys(holder.address());
            whoseTransfers.sendKeys(Keys.ENTER);
            Thread.sleep(200);
            whoseTransfers.clear();
        }

        List<WebElement> usdInputs = driver.findElements(By.xpath("//input[@placeholder='No minimum']"));
        WebElement usdMin = usdInputs.getFirst();
        usdMin.click();
        usdMin.sendKeys("900000");

        WebElement tokenInput = driver.findElement(By.xpath("//input[@placeholder='Search for a token']"));
        tokenInput.sendKeys(info.coinName());
        Thread.sleep(1000);
        tokenInput.sendKeys(Keys.ENTER);

        WebElement chainDropdown = driver.findElement(By.xpath("//div[contains(@class,'sortOptionsCurrent')]"));
        chainDropdown.click();
        WebElement chain = driver.findElement(By.xpath("//div[contains(text(),'" + info.network().toLowerCase() + "')]"));
        chain.click();
        alertName.click();

        WebElement telegram = driver.findElement(By.xpath("//span[contains(text(),'@oleh_dolinin')]"));
        telegram.click();

        WebElement create = driver.findElement(By.xpath("//button[contains(text(),'CREATE ALERT')]"));
        create.click();
    }

    private boolean searchToken(WebDriver driver, String token) throws Exception {
        driver.get("https://intel.arkm.com/explorer/token/lombard-protocol");
        Thread.sleep(TARGET_SLEEP);

        WebElement searchInput = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT))
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("input.Input-module__j8lwcG__searchInput")));

        searchInput.click();
        searchInput.sendKeys(token);
        Thread.sleep(1000);

        List<WebElement> headers = driver.findElements(By.cssSelector("div.Input-module__j8lwcG__suggestionHeaderSearch"));

        for (WebElement header : headers) {
            if (header.getText().trim().equals("TOKENS")) {
                List<WebElement> tokens = driver.findElements(By.xpath(
                        "//div[contains(@class,'Input-module__j8lwcG__suggestions')]/div" +
                                "[preceding-sibling::div[contains(@class,'Input-module__j8lwcG__suggestionHeaderSearch') and normalize-space(text())='TOKENS'] " +
                                "and not(contains(@class,'Input-module__j8lwcG__suggestionHeaderSearch'))]"
                ));

                if (!tokens.isEmpty()) {
                    tokens.getFirst().click();
                    Thread.sleep(TARGET_SLEEP);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private boolean alertExists(WebDriver driver, String alertName) {
        JavascriptExecutor js = (JavascriptExecutor) driver;

        return (Boolean) js.executeScript("""
                    const name = arguments[0].toLowerCase();
                    const items = document.querySelectorAll('.NavBar-module__8u-qnq__name');
                
                    return Array.from(items)
                        .map(e => e.innerText.trim().toLowerCase())
                        .includes(name);
                """, alertName);
    }
}
# Arkham Top Holders Alert Bot

## Description

This project is a **Spring Boot automation bot** that works with **Arkham Intelligence** using **Selenium WebDriver**.

The bot automatically:

* Retrieves a list of crypto tokens from MEXC
* Opens each token page on Arkham
* Filters tokens by **market cap**
* Finds **top holders**
* Tracks wallets holding large amounts
* Automatically creates **alerts** for large transactions

The main goal of the project is to monitor **large wallet movements** that can affect the market.

---

# Features

* Token list collection
* Market cap filtering

    * Minimum: **$10,000,000**
    * Maximum: **$10,000,000,000**
* Automatic navigation on Arkham
* Top holder parsing
* Wallet filtering by value

    * Minimum holder balance: **$30,000,000**
* Automatic alert creation
* Alert cleanup (delete existing alerts before creating new ones)

---

# Tech Stack

* **Java**
* **Spring Boot**
* **Selenium WebDriver**
* **Maven**
* **Chrome Remote Debugging**

---

# Requirements

* Java **21+**
* Maven
* Google Chrome
* ChromeDriver

---

# Important Setup (Required)

To avoid bot detection and repeated logins, the bot uses a **persistent Chrome profile**.

## 1. Create a Chrome profile folder

Create the following folder on your system:

```
C:\selenium-profile
```

---

## 2. Start Chrome with remote debugging

Open **Command Prompt (cmd)** and run:

```
"C:\Program Files\Google\Chrome\Application\chrome.exe" --remote-debugging-port=9222 --user-data-dir="C:\selenium-profile"
```

This will launch Chrome using the dedicated automation profile.

---

## 3. Login manually

After Chrome opens:

1. Go to **https://intel.arkm.com**
2. Log into your Arkham account
3. Make sure everything loads correctly

The login session will now be saved inside:

```
C:\selenium-profile
```

From this point the bot will reuse the session and **no longer require authentication**.

---

# Run the Project

Clone the repository:

```
git clone https://github.com/njdos/parser_listing_bot.git
```

Go to the project folder:

```
cd project-name
```

Run the project:

```
mvn spring-boot:run
```

---

# How It Works

1. The bot loads a list of tokens.
2. Each token page is opened on **Arkham**.
3. The bot reads token information:

    * Name
    * Network
    * Market cap
4. Tokens outside the range **$10M – $10B** are skipped.
5. Top holders are parsed.
6. Wallets with **≥ $1M holdings** are selected.
7. Alerts are automatically created for wallet transfers.

---

# Project Structure

```
src/main/java/com/example/parser_bithumb_bot

services/
    ParserService.java
    MexcService.java

models/
    CoinInfo.java
    TopHolder.java

config/
    WebDriverConfig.java
```

---

# Notes

This bot relies on **browser automation**, so the Chrome instance must be running with the configured profile.

Make sure:

* ChromeDriver version matches your Chrome version
* Chrome is started with **remote debugging**
* You are logged into Arkham

---

# Disclaimer

This project is intended for **educational and research purposes only**.

Use responsibly when interacting with third-party platforms.

---

# Future Improvements

* Telegram alert notifications
* Database for wallet tracking
* Parallel token processing
* Automatic discovery of new large wallets
* Advanced filtering for whale activity

---

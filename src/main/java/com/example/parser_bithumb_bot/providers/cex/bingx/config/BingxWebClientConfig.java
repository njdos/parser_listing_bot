package com.example.parser_bithumb_bot.providers.cex.bingx.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class BingxWebClientConfig {

    @Bean
    public WebClient bingxWebClient() {

        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .keepAlive(true)
                .responseTimeout(Duration.ofSeconds(2))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);

        return WebClient.builder()
                .baseUrl("https://open-api.bingx.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
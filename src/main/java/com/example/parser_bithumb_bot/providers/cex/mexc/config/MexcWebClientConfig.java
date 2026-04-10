package com.example.parser_bithumb_bot.providers.cex.mexc.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class MexcWebClientConfig {

    @Bean
    public WebClient mexcWebClient() {

        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .keepAlive(true)
                .responseTimeout(Duration.ofSeconds(2))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);

        return WebClient.builder()
                .baseUrl("https://api.mexc.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

}
package com.example.parser_bithumb_bot.providers.cex.okx.config;

import io.netty.channel.ChannelOption;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class OkxWebClientConfig {

    @Bean
    public WebClient okxWebClient() {
        HttpClient httpClient = HttpClient.create()
                .compress(true)
                .keepAlive(true)
                .responseTimeout(Duration.ofSeconds(2))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);

        return WebClient.builder()
                .baseUrl("https://www.okx.com")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebSocketClient okxWsClient() {
        return new ReactorNettyWebSocketClient();
    }

}
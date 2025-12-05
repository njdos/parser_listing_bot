package com.example.parser_bithumb_bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ParserBithumbBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(ParserBithumbBotApplication.class, args);
    }

}

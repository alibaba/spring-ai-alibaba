package com.alibaba.cloud.ai.example.cli.clidebugexample;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.alibaba.cloud.ai"})
@SpringBootApplication
public class CliDebugExampleApplication {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder.build();
    }

    public static void main(String[] args) {
        SpringApplication.run(CliDebugExampleApplication.class, args);
    }

}

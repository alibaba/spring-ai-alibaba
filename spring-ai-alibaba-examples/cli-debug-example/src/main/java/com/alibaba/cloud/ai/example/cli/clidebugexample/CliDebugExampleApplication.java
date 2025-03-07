package com.alibaba.cloud.ai.example.cli.clidebugexample;

import com.alibaba.cloud.ai.example.cli.clidebugexample.function.MockWeatherService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({"com.alibaba.cloud.ai"})
@SpringBootApplication
public class CliDebugExampleApplication {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        return builder
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
            .build();
    }

    @Bean
    ChatClient weatherChatClient(ChatClient.Builder builder) {
        ChatMemory chatMemory = new InMemoryChatMemory();
        return builder
            .defaultFunction("getWeather", "根据城市查询天气", new MockWeatherService())
            .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
            .build();
    }

    public static void main(String[] args) {
        SpringApplication.run(CliDebugExampleApplication.class, args);
    }

}

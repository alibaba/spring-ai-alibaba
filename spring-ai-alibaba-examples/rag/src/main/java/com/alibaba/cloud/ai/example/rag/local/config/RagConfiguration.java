package com.alibaba.cloud.ai.example.rag.local.config;

import com.alibaba.cloud.ai.example.rag.local.RagService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfiguration {

	@Bean
	public RagService ragService(ChatClient.Builder builder, EmbeddingModel embeddingModel) {
		return new RagService(builder.build(), embeddingModel);
	}

}

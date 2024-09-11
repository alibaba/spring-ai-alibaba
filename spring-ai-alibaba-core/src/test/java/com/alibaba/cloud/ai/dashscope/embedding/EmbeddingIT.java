package com.alibaba.cloud.ai.dashscope.embedding;

import java.util.List;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeAutoConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashScopeAutoConfiguration.class)
public class EmbeddingIT {

	@Autowired
	private DashScopeEmbeddingModel dashscopeEmbeddingModel;

	@Test
	void defaultEmbedding() {
		assertThat(dashscopeEmbeddingModel).isNotNull();

		EmbeddingResponse embeddingResponse = dashscopeEmbeddingModel.embedForResponse(List.of("hello world"));
		System.out.println(embeddingResponse);
		assertThat(embeddingResponse.getResults()).hasSize(1);
		assertThat(embeddingResponse.getResults().get(0)).isNotNull();
		assertThat(embeddingResponse.getResults().get(0).getOutput()).hasSize(1536);
		Assertions.assertEquals(embeddingResponse.getMetadata().get("model"), "text-embedding-v1");
		Assertions.assertEquals((Long) embeddingResponse.getMetadata().get("total-tokens"), 2);
	}

}

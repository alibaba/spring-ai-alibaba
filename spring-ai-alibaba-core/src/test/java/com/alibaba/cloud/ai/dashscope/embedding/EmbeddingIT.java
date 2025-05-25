/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.dashscope.embedding;

import java.util.List;

import com.alibaba.cloud.ai.autoconfig.dashscope.DashScopeAutoConfiguration;
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

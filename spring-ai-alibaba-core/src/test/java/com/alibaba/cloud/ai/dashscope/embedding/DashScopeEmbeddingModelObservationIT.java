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

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.observation.conventions.AiProvider;
import io.micrometer.observation.tck.TestObservationRegistry;
import io.micrometer.observation.tck.TestObservationRegistryAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.ai.embedding.observation.DefaultEmbeddingModelObservationConvention;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.LowCardinalityKeyNames;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationDocumentation.HighCardinalityKeyNames;
import org.springframework.ai.observation.conventions.AiOperationType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for observation instrumentation in {@link DashScopeEmbeddingModel}.
 *
 * @author Lumian
 */
@SpringBootTest(classes = DashScopeEmbeddingModelObservationIT.Config.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
public class DashScopeEmbeddingModelObservationIT {

	@Autowired
	TestObservationRegistry observationRegistry;

	@Autowired
	DashScopeEmbeddingModel dashscopeEmbeddingModel;

	@Test
	void observationForEmbeddingOperation() {
		var options = DashScopeEmbeddingOptions.builder()
			.withModel(DashScopeApi.EmbeddingModel.EMBEDDING_V3.getValue())
			.withDimensions(1536)
			.withTextType(DashScopeApi.EmbeddingTextType.QUERY.getValue())
			.build();

		EmbeddingRequest embeddingRequest = new EmbeddingRequest(
				List.of("The clothes are of good quality and look good, definitely worth the wait. I love them."),
				options);

		EmbeddingResponse embeddingResponse = this.dashscopeEmbeddingModel.call(embeddingRequest);
		assertThat(embeddingResponse.getResults()).isNotEmpty();

		EmbeddingResponseMetadata responseMetadata = embeddingResponse.getMetadata();
		assertThat(responseMetadata).isNotNull();

		TestObservationRegistryAssert.assertThat(this.observationRegistry)
			.doesNotHaveAnyRemainingCurrentObservation()
			.hasObservationWithNameEqualTo(DefaultEmbeddingModelObservationConvention.DEFAULT_NAME)
			.that()
			.hasContextualNameEqualTo("embedding " + DashScopeApi.EmbeddingModel.EMBEDDING_V3.getValue())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_OPERATION_TYPE.asString(),
					AiOperationType.EMBEDDING.value())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.AI_PROVIDER.asString(), AiProvider.DASHSCOPE.value())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.REQUEST_MODEL.asString(),
					DashScopeApi.EmbeddingModel.EMBEDDING_V3.getValue())
			.hasLowCardinalityKeyValue(LowCardinalityKeyNames.RESPONSE_MODEL.asString(), responseMetadata.getModel())
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.REQUEST_EMBEDDING_DIMENSIONS.asString(), "1536")
			.doesNotHaveHighCardinalityKeyValueWithKey(HighCardinalityKeyNames.USAGE_INPUT_TOKENS.asString())
			.hasHighCardinalityKeyValue(HighCardinalityKeyNames.USAGE_TOTAL_TOKENS.asString(),
					String.valueOf(responseMetadata.getUsage().getTotalTokens()))
			.hasBeenStarted()
			.hasBeenStopped();
	}

	@SpringBootConfiguration
	static class Config {

		@Bean
		public TestObservationRegistry observationRegistry() {
			return TestObservationRegistry.create();
		}

		@Bean
		public DashScopeApi dashScopeApi() {
			return new DashScopeApi(System.getenv("DASHSCOPE_API_KEY"));
		}

		@Bean
		public DashScopeEmbeddingModel dashScopeEmbeddingModel(DashScopeApi dashScopeApi,
				TestObservationRegistry observationRegistry) {
			return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED,
					DashScopeEmbeddingOptions.builder().build(), RetryTemplate.defaultInstance(), observationRegistry);
		}

	}

}

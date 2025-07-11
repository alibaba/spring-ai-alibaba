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

package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioSpeechModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.rerank.DashScopeRerankModel;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author YunKui Lu
 * @author yuluo
 */
class DashScopeModelConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.ai.dashscope.api-key=API_KEY");

	@Test
	void chatModelActivation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.chat=none")
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatProperties.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.chat=dashscope")
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isNotEmpty();
			});

		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class,
					DashScopeEmbeddingAutoConfiguration.class, DashScopeImageAutoConfiguration.class,
					DashScopeAudioSpeechAutoConfiguration.class, DashScopeAudioTranscriptionAutoConfiguration.class,
					DashScopeRerankAutoConfiguration.class))
			.withPropertyValues(
			// @formatter:off
					"spring.ai.model.chat=dashscope",
					"spring.ai.model.embedding=none",
					"spring.ai.model.image=none",
					"spring.ai.model.audio.speech=none",
					"spring.ai.model.audio.transcription=none",
					"spring.ai.model.moderation=none",
					"spring.ai.model.rerank=none")
			// @formatter:on
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});
	}

	@Test
	void embeddingModelActivation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.embedding=none")
			.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeEmbeddingProperties.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.embedding=dashscope")
			.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeEmbeddingProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isNotEmpty();
			});

		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class,
					DashScopeEmbeddingAutoConfiguration.class, DashScopeImageAutoConfiguration.class,
					DashScopeAudioSpeechAutoConfiguration.class, DashScopeAudioTranscriptionAutoConfiguration.class,
					DashScopeRerankAutoConfiguration.class))
			.withPropertyValues(
			// @formatter:off
					"spring.ai.model.chat=none",
					"spring.ai.model.embedding=dashscope",
					"spring.ai.model.image=none",
					"spring.ai.model.audio.speech=none",
					"spring.ai.model.audio.transcription=none",
					"spring.ai.model.moderation=none",
					"spring.ai.model.rerank=none")
				// @formatter
			.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});
	}

	@Test
	void imageModelActivation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DashScopeImageAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.image=none")
			.withConfiguration(AutoConfigurations.of(DashScopeImageAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeImageProperties.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.image=dashscope")
			.withConfiguration(AutoConfigurations.of(DashScopeImageAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeImageProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isNotEmpty();
			});

		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class,
					DashScopeEmbeddingAutoConfiguration.class, DashScopeImageAutoConfiguration.class,
					DashScopeAudioSpeechAutoConfiguration.class, DashScopeAudioTranscriptionAutoConfiguration.class,
					DashScopeRerankAutoConfiguration.class))
			.withPropertyValues(
					// @formatter:off
					"spring.ai.model.chat=none",
					"spring.ai.model.embedding=none",
					"spring.ai.model.image=dashscope",
					"spring.ai.model.audio.speech=none",
					"spring.ai.model.audio.transcription=none",
					"spring.ai.model.moderation=none",
					"spring.ai.model.rerank=none")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});
	}

	@Test
	void audioSpeechModelActivation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DashScopeAudioSpeechAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.audio.speech=none")
			.withConfiguration(AutoConfigurations.of(DashScopeAudioSpeechAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeAudioSpeechSynthesisProperties.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.audio.speech=dashscope")
			.withConfiguration(AutoConfigurations.of(DashScopeAudioSpeechAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeAudioSpeechSynthesisProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isNotEmpty();
			});

		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class,
					DashScopeEmbeddingAutoConfiguration.class, DashScopeImageAutoConfiguration.class,
					DashScopeAudioSpeechAutoConfiguration.class, DashScopeAudioTranscriptionAutoConfiguration.class,
					DashScopeRerankAutoConfiguration.class))
			.withPropertyValues(
			// @formatter:off
					"spring.ai.model.chat=none",
					"spring.ai.model.embedding=none",
					"spring.ai.model.image=none",
					"spring.ai.model.audio.speech=dashscope",
					"spring.ai.model.audio.transcription=none",
					"spring.ai.model.moderation=none",
					"spring.ai.model.rerank=none")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});
	}

	@Test
	void audioTranscriptionModelActivation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DashScopeAudioTranscriptionAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.audio.transcription=none")
			.withConfiguration(AutoConfigurations.of(DashScopeAudioTranscriptionAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionProperties.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.audio.transcription=dashscope")
			.withConfiguration(AutoConfigurations.of(DashScopeAudioTranscriptionAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isNotEmpty();
			});

		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class,
					DashScopeEmbeddingAutoConfiguration.class, DashScopeImageAutoConfiguration.class,
					DashScopeAudioSpeechAutoConfiguration.class, DashScopeAudioTranscriptionAutoConfiguration.class,
					DashScopeRerankAutoConfiguration.class))
			.withPropertyValues(
			// @formatter:off
					"spring.ai.model.chat=none",
					"spring.ai.model.embedding=none",
					"spring.ai.model.image=none",
					"spring.ai.model.audio.speech=none",
					"spring.ai.model.audio.transcription=dashscope",
					"spring.ai.model.moderation=none",
					"spring.ai.model.rerank=none")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});
	}

	@Test
	void rerankModelActivation() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(DashScopeRerankAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isNotEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.rerank=none")
			.withConfiguration(AutoConfigurations.of(DashScopeRerankAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeRerankProperties.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isEmpty();
			});

		this.contextRunner.withPropertyValues("spring.ai.model.rerank=dashscope")
			.withConfiguration(AutoConfigurations.of(DashScopeRerankAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeRerankProperties.class)).isNotEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isNotEmpty();
			});

		this.contextRunner
			.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class,
					DashScopeEmbeddingAutoConfiguration.class, DashScopeImageAutoConfiguration.class,
					DashScopeAudioSpeechAutoConfiguration.class, DashScopeAudioTranscriptionAutoConfiguration.class,
					DashScopeRerankAutoConfiguration.class))
			.withPropertyValues(
			// @formatter:off
					"spring.ai.model.chat=none",
					"spring.ai.model.embedding=none",
					"spring.ai.model.image=none",
					"spring.ai.model.audio.speech=none",
					"spring.ai.model.audio.transcription=none",
					"spring.ai.model.moderation=none",
					"spring.ai.model.rerank=dashscope")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class))
			.run(context -> {
				assertThat(context.getBeansOfType(DashScopeChatModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeEmbeddingModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeImageModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioSpeechModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeAudioTranscriptionModel.class)).isEmpty();
				assertThat(context.getBeansOfType(DashScopeRerankModel.class)).isNotEmpty();
			});
	}

}

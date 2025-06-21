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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".*")
public class DashScopeAutoConfigurationIT {

	private static final Log logger = LogFactory.getLog(DashScopeAutoConfigurationIT.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.ai.dashscope.api-key=" + System.getenv("AI_DASHSCOPE_API_KEY"))
		.withConfiguration(AutoConfigurations.of(DashScopeChatAutoConfiguration.class));

	private final ApplicationContextRunner textEmbeddingDefaultContextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.ai.dashscope.api-key=" + System.getenv("AI_DASHSCOPE_API_KEY"))
		.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class));

	private final ApplicationContextRunner textEmbeddingV3ContextRunner = new ApplicationContextRunner()
		.withPropertyValues("spring.ai.dashscope.api-key=" + System.getenv("AI_DASHSCOPE_API_KEY"))
		.withPropertyValues("spring.ai.dashscope.embedding.options.model=text-embedding-v3")
		.withPropertyValues("spring.ai.dashscope.embedding.options.dimensions=512")
		.withConfiguration(AutoConfigurations.of(DashScopeEmbeddingAutoConfiguration.class));

	@Test
	void chatCall() {

		this.contextRunner.run(context -> {
			DashScopeChatModel chatModel = context.getBean(DashScopeChatModel.class);
			String response = chatModel.call("Hello");
			assertThat(response).isNotEmpty();
			logger.info("Response: " + response);
		});
	}

	// https://github.com/alibaba/spring-ai-alibaba/issues/295
	// @Test
	// void transcribe() {
	//
	// this.contextRunner.run(context -> {
	// DashScopeAudioTranscriptionModel transcriptionModel =
	// context.getBean(DashScopeAudioTranscriptionModel.class);
	// Resource audioFile = new ClassPathResource("/speech/jfk.flac");
	// System.out.println(audioFile);
	// String response = transcriptionModel.call(new
	// AudioTranscriptionPrompt(audioFile)).getResult().getOutput();
	// System.out.println(response);
	// assertThat(response).isNotEmpty();
	// logger.info("Response: " + response);
	// });
	// }

	@Test
	void speech() {
		this.contextRunner.run(context -> {
			DashScopeSpeechSynthesisModel speechModel = context.getBean(DashScopeSpeechSynthesisModel.class);
			byte[] response = speechModel
				.call(new SpeechSynthesisPrompt("H",
						DashScopeSpeechSynthesisOptions.builder()
							.responseFormat(DashScopeSpeechSynthesisApi.ResponseFormat.MP3)
							.build()))
				.getResult()
				.getOutput()
				.getAudio()
				.array();
			assertThat(response).isNotNull();
			// todo: check mp3 types
			assertThat(response.length).isNotEqualTo(0);

			logger.debug("Response: " + Arrays.toString(response));
		});
	}

	@Test
	void generateStreaming() {
		this.contextRunner.run(context -> {
			DashScopeChatModel chatModel = context.getBean(DashScopeChatModel.class);
			Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt(new UserMessage("Hello")));
			String response = Objects.requireNonNull(responseFlux.collectList().block())
				.stream()
				.map(chatResponse -> chatResponse.getResults().get(0).getOutput().getText())
				.collect(Collectors.joining());

			assertThat(response).isNotEmpty();
			logger.info("Response: " + response);
		});
	}

	@Test
	void streamingWithTokenUsage() {
		this.contextRunner.withPropertyValues("spring.ai.dashScope.chat").run(context -> {
			DashScopeChatModel chatModel = context.getBean(DashScopeChatModel.class);

			Flux<ChatResponse> responseFlux = chatModel.stream(new Prompt(new UserMessage("Hello")));

			Usage[] streamingTokenUsage = new Usage[1];
			String response = Objects.requireNonNull(responseFlux.collectList().block()).stream().map(chatResponse -> {
				streamingTokenUsage[0] = chatResponse.getMetadata().getUsage();
				return (chatResponse.getResult() != null) ? chatResponse.getResult().getOutput().getText() : "";
			}).collect(Collectors.joining());

			assertThat(streamingTokenUsage[0].getPromptTokens()).isGreaterThan(0);
			assertThat(streamingTokenUsage[0].getCompletionTokens()).isGreaterThan(0);
			assertThat(streamingTokenUsage[0].getTotalTokens()).isGreaterThan(0);

			assertThat(response).isNotEmpty();
			logger.info("Response: " + response);
		});
	}

	@Test
	void embedding() {
		this.textEmbeddingDefaultContextRunner.run(context -> {
			DashScopeEmbeddingModel embeddingModel = context.getBean(DashScopeEmbeddingModel.class);

			EmbeddingResponse embeddingResponse = embeddingModel
				.embedForResponse(List.of("Hello World", "World is big and salvation is near"));
			assertThat(embeddingResponse.getResults()).hasSize(2);
			assertThat(embeddingResponse.getResults().get(0).getOutput()).isNotEmpty();
			assertThat(embeddingResponse.getResults().get(0).getIndex()).isEqualTo(0);
			assertThat(embeddingResponse.getResults().get(1).getOutput()).isNotEmpty();
			assertThat(embeddingResponse.getResults().get(1).getIndex()).isEqualTo(1);

			assertThat(embeddingModel.dimensions()).isEqualTo(1536);
		});
	}

	@Test
	void embeddingWithTextEmbeddingV3Mode() {
		this.textEmbeddingV3ContextRunner.run(context -> {
			DashScopeEmbeddingModel embeddingModel = context.getBean(DashScopeEmbeddingModel.class);

			EmbeddingResponse embeddingResponse = embeddingModel
				.embedForResponse(List.of("Hello World", "World is big and salvation is near"));
			assertThat(embeddingResponse.getResults()).hasSize(2);
			assertThat(embeddingResponse.getResults().get(0).getOutput()).isNotEmpty();
			assertThat(embeddingResponse.getResults().get(0).getIndex()).isEqualTo(0);
			assertThat(embeddingResponse.getResults().get(1).getOutput()).isNotEmpty();
			assertThat(embeddingResponse.getResults().get(1).getIndex()).isEqualTo(1);

			assertThat(embeddingModel.dimensions()).isEqualTo(512);

		});
	}

	@Test
	void generateImage() {
		this.contextRunner.withPropertyValues("spring.ai.dashScope.image.options.size=1024x1024").run(context -> {
			DashScopeImageModel imageModel = context.getBean(DashScopeImageModel.class);
			ImageResponse imageResponse = imageModel.call(new ImagePrompt("tree"));
			System.out.println(imageResponse.getResult().getOutput().getUrl());
			assertThat(imageResponse.getResults()).hasSize(1);
			assertThat(imageResponse.getResult().getOutput().getUrl()).isNotEmpty();
			logger.info("Generated image: " + imageResponse.getResult().getOutput().getUrl());
		});
	}

	@Test
	void generateImageWithModel() {
		// The 256x256 size is supported by dall-e-2, but not by dall-e-3.
		this.contextRunner
			.withPropertyValues("spring.ai.dashScope.image.options.model=wanx-v1",
					"spring.ai.dashScope.image.options.size=256x256")
			.run(context -> {
				DashScopeImageModel imageModel = context.getBean(DashScopeImageModel.class);
				ImageResponse imageResponse = imageModel.call(new ImagePrompt("tree"));
				assertThat(imageResponse.getResults()).hasSize(1);
				assertThat(imageResponse.getResult().getOutput().getUrl()).isNotEmpty();
				logger.info("Generated image: " + imageResponse.getResult().getOutput().getUrl());
			});
	}

}

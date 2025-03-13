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

import java.util.List;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeSpeechSynthesisApi;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionModel;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeAudioTranscriptionOptions;
import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public class DashScopePropertiesTests {

	@Test
	public void chatProperties() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.chat.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.chat.options.temperature=0.80")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var chatProperties = context.getBean(DashScopeChatProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(chatProperties.getApiKey()).isNull();
				assertThat(chatProperties.getBaseUrl()).isEqualTo(DEFAULT_BASE_URL);

				assertThat(chatProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				assertThat(chatProperties.getOptions().getTemperature()).isEqualTo(0.80);
			});
	}

	@Test
	public void transcriptionProperties() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.audio.transcription.options.model=MODEL_CUSTOM")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var transcriptionProperties = context.getBean(DashScopeAudioTranscriptionProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(transcriptionProperties.getApiKey()).isNull();
				assertThat(transcriptionProperties.getBaseUrl()).isEqualTo(DEFAULT_BASE_URL);

				assertThat(transcriptionProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
			});
	}

	@Test
	public void chatOverrideConnectionProperties() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.chat.base-url=TEST_BASE_URL2",
						"spring.ai.dashscope.chat.api-key=456",
						"spring.ai.dashscope.chat.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.chat.options.temperature=0.88")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var chatProperties = context.getBean(DashScopeChatProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(chatProperties.getApiKey()).isEqualTo("456");
				assertThat(chatProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL2");

				assertThat(chatProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				assertThat(chatProperties.getOptions().getTemperature()).isEqualTo(0.88);
			});
	}

	@Test
	public void transcriptionOverrideConnectionProperties() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.audio.transcription.base-url=TEST_BASE_URL2",
						"spring.ai.dashscope.audio.transcription.api-key=456",
						"spring.ai.dashscope.audio.transcription.options.model=MODEL_CUSTOM")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var transcriptionProperties = context.getBean(DashScopeAudioTranscriptionProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(transcriptionProperties.getApiKey()).isEqualTo("456");
				assertThat(transcriptionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL2");

				assertThat(transcriptionProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
			});
	}

	@Test
	public void speechProperties() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.audio.synthesis.options.model=TTS_1",
						"spring.ai.dashscope.audio.synthesis.options.voice=longhua_test",
						"spring.ai.dashscope.audio.synthesis.options.response-format=mp3",
						"spring.ai.dashscope.audio.synthesis.options.speed=0.75")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var speechProperties = context.getBean(DashScopeSpeechSynthesisProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(speechProperties.getApiKey()).isNull();
				assertThat(speechProperties.getBaseUrl()).isEqualTo(DEFAULT_BASE_URL);

				assertThat(speechProperties.getOptions().getModel()).isEqualTo("TTS_1");
				assertThat(speechProperties.getOptions().getVoice()).isEqualTo("longhua_test");
				assertThat(speechProperties.getOptions().getResponseFormat())
					.isEqualTo(DashScopeSpeechSynthesisApi.ResponseFormat.MP3);
				assertThat(speechProperties.getOptions().getSpeed()).isEqualTo(0.75f);
			});
	}

	@Test
	public void speechOverrideConnectionPropertiesTest() {
		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.audio.synthesis.base-url=TEST_BASE_URL2",
						"spring.ai.dashscope.audio.synthesis.api-key=456",
						"spring.ai.dashscope.audio.synthesis.options.model=TTS_2",
						"spring.ai.dashscope.audio.synthesis.options.voice=echo",
						"spring.ai.dashscope.audio.synthesis.options.response-format=pcm",
						"spring.ai.dashscope.audio.synthesis.options.speed=0.8")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var speechProperties = context.getBean(DashScopeSpeechSynthesisProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(speechProperties.getApiKey()).isEqualTo("456");
				assertThat(speechProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL2");

				assertThat(speechProperties.getOptions().getModel()).isEqualTo("TTS_2");
				assertThat(speechProperties.getOptions().getVoice()).isEqualTo("echo");
				assertThat(speechProperties.getOptions().getResponseFormat())
					.isEqualTo(DashScopeSpeechSynthesisApi.ResponseFormat.PCM);
				assertThat(speechProperties.getOptions().getSpeed()).isEqualTo(0.8);
			});
	}

	@Test
	public void embeddingProperties() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.embedding.options.model=MODEL_CUSTOM")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var embeddingProperties = context.getBean(DashScopeEmbeddingProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(embeddingProperties.getApiKey()).isNull();
				assertThat(embeddingProperties.getBaseUrl()).isNull();

				assertThat(embeddingProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
			});
	}

	@Test
	public void embeddingOverrideConnectionProperties() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.embedding.base-url=TEST_BASE_URL2",
						"spring.ai.dashscope.embedding.api-key=456",
						"spring.ai.dashscope.embedding.options.model=MODEL_CUSTOM")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var embeddingProperties = context.getBean(DashScopeEmbeddingProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(embeddingProperties.getApiKey()).isEqualTo("456");
				assertThat(embeddingProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL2");

				assertThat(embeddingProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
			});
	}

	@Test
	public void imageProperties() {
		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.image.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.image.options.n=3")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var imageProperties = context.getBean(DashScopeImageProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(imageProperties.getApiKey()).isNull();
				assertThat(imageProperties.getBaseUrl()).isNull();

				assertThat(imageProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				assertThat(imageProperties.getOptions().getN()).isEqualTo(3);
			});
	}

	@Test
	public void imageOverrideConnectionProperties() {
		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.base-url=TEST_BASE_URL",
						"spring.ai.dashscope.api-key=abc123_test",
						"spring.ai.dashscope.image.base-url=TEST_BASE_URL2",
						"spring.ai.dashscope.image.api-key=456",
						"spring.ai.dashscope.image.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.image.options.n=3")
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var imageProperties = context.getBean(DashScopeImageProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getApiKey()).isEqualTo("abc123_test");
				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");

				assertThat(imageProperties.getApiKey()).isEqualTo("456");
				assertThat(imageProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL2");

				assertThat(imageProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				assertThat(imageProperties.getOptions().getN()).isEqualTo(3);
			});
	}

	@Test
	public void chatOptionsTest() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.api-key=API_KEY",
						"spring.ai.dashscope.base-url=TEST_BASE_URL",

						"spring.ai.dashscope.chat.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.chat.options.seed=66",
						"spring.ai.dashscope.chat.options.stop=boza,koza",
						"spring.ai.dashscope.chat.options.temperature=0.88",
						"spring.ai.dashscope.chat.options.topP=0.56",

						// "spring.ai.dashscope.chat.options.toolChoice.functionName=toolChoiceFunctionName",
						"spring.ai.dashscope.chat.options.toolChoice=" + ModelOptionsUtils.toJsonString(DashScopeApi.ChatCompletionRequestParameter.ToolChoiceBuilder.function("toolChoiceFunctionName"))
				)
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var chatProperties = context.getBean(DashScopeChatProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);
				var embeddingProperties = context.getBean(DashScopeEmbeddingProperties.class);

				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");
				assertThat(connectionProperties.getApiKey()).isEqualTo("API_KEY");

				assertThat(embeddingProperties.getOptions().getModel())
					.isEqualTo(DashScopeApi.EmbeddingModel.EMBEDDING_V1.getValue());

				assertThat(chatProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				assertThat(chatProperties.getOptions().getSeed()).isEqualTo(66);
				List<Object> stop = chatProperties.getOptions().getStop();
				assertThat(stop.contains("boza")).isTrue();
				assertThat(stop.contains("koza")).isTrue();
				assertThat(chatProperties.getOptions().getTemperature()).isEqualTo(0.88);
				assertThat(chatProperties.getOptions().getTopP()).isEqualTo(0.56);

				JSONAssert.assertEquals("{\"type\":\"function\",\"function\":{\"name\":\"toolChoiceFunctionName\"}}",
						"" + chatProperties.getOptions().getToolChoice(), JSONCompareMode.LENIENT);

				// 注意：我们不测试 tools 属性，因为它需要通过代码设置，而不是通过配置属性
			});
	}

	@Test
	public void transcriptionOptionsTest() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.api-key=API_KEY",
						"spring.ai.dashscope.base-url=TEST_BASE_URL",

						"spring.ai.dashscope.audio.transcription.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.audio.transcription.options.language-hints=en",
						"spring.ai.dashscope.audio.transcription.options.format=mp3",
						"spring.ai.dashscope.audio.transcription.options.temperature=0.88"
				)
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var transcriptionProperties = context.getBean(DashScopeAudioTranscriptionProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);
				var embeddingProperties = context.getBean(DashScopeEmbeddingProperties.class);

				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");
				assertThat(connectionProperties.getApiKey()).isEqualTo("API_KEY");

				assertThat(embeddingProperties.getOptions().getModel())
					.isEqualTo(DashScopeApi.EmbeddingModel.EMBEDDING_V1.getValue());

				assertThat(transcriptionProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				List<String> languageHints = transcriptionProperties.getOptions().getLanguageHints();
				assertThat(languageHints.contains("en")).isTrue();
				assertThat(transcriptionProperties.getOptions().getFormat())
					.isEqualTo(DashScopeAudioTranscriptionOptions.AudioFormat.MP3);
			});
	}

	@Test
	public void embeddingOptionsTest() {

		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.api-key=API_KEY",
						"spring.ai.dashscope.base-url=TEST_BASE_URL",

						"spring.ai.dashscope.embedding.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.embedding.options.text-type=text"
				)
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);
				var embeddingProperties = context.getBean(DashScopeEmbeddingProperties.class);

				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");
				assertThat(connectionProperties.getApiKey()).isEqualTo("API_KEY");

				assertThat(embeddingProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				assertThat(embeddingProperties.getOptions().getTextType()).isEqualTo("text");
			});
	}

	@Test
	public void imageOptionsTest() {
		new ApplicationContextRunner().withPropertyValues(
		// @formatter:off
						"spring.ai.dashscope.api-key=API_KEY",
						"spring.ai.dashscope.base-url=TEST_BASE_URL",

						"spring.ai.dashscope.image.options.n=3",
						"spring.ai.dashscope.image.options.model=MODEL_CUSTOM",
						"spring.ai.dashscope.image.options.size=10*10",
						"spring.ai.dashscope.image.options.width=10",
						"spring.ai.dashscope.image.options.height=10",
						"spring.ai.dashscope.image.options.style=vivid"
				)
				// @formatter:on
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				var imageProperties = context.getBean(DashScopeImageProperties.class);
				var connectionProperties = context.getBean(DashScopeConnectionProperties.class);

				assertThat(connectionProperties.getBaseUrl()).isEqualTo("TEST_BASE_URL");
				assertThat(connectionProperties.getApiKey()).isEqualTo("API_KEY");

				assertThat(imageProperties.getOptions().getN()).isEqualTo(3);
				assertThat(imageProperties.getOptions().getModel()).isEqualTo("MODEL_CUSTOM");
				assertThat(imageProperties.getOptions().getSize()).isEqualTo("10*10");
				assertThat(imageProperties.getOptions().getWidth()).isEqualTo(10);
				assertThat(imageProperties.getOptions().getHeight()).isEqualTo(10);
				assertThat(imageProperties.getOptions().getStyle()).isEqualTo("vivid");
			});
	}

	@Test
	void embeddingActivation() {

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.embedding.enabled=false")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeEmbeddingProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeEmbeddingModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeEmbeddingProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeEmbeddingModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.embedding.enabled=true")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeEmbeddingProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeEmbeddingModel.class));
			});
	}

	@Test
	void chatActivation() {
		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.chat.enabled=false")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeChatProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeChatModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeChatProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeChatModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.chat.enabled=true")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeChatProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeChatModel.class));
			});

	}

	@Test
	void imageActivation() {
		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.image.enabled=false")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeImageProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeImageModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeImageProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeImageModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.image.enabled=true")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeImageProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeImageModel.class));
			});

	}

	@Test
	void audioSpeechActivation() {
		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.audio.speech.enabled=false")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeSpeechSynthesisProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeSpeechSynthesisModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeSpeechSynthesisProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeSpeechSynthesisModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.audio.speech.enabled=true")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeSpeechSynthesisProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeSpeechSynthesisModel.class));
			});

	}

	@Test
	void audioTranscriptionActivation() {
		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.audio.transcription.enabled=false")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeAudioTranscriptionProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeAudioTranscriptionModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeAudioTranscriptionProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeAudioTranscriptionModel.class));
			});

		new ApplicationContextRunner()
			.withPropertyValues("spring.ai.dashscope.api-key=API_KEY", "spring.ai.dashscope.base-url=TEST_BASE_URL",
					"spring.ai.dashscope.audio.transcription.enabled=true")
			.withConfiguration(AutoConfigurations.of(DashScopeAutoConfiguration.class))
			.run(context -> {
				assertNotNull(context.getBeansOfType(DashScopeAudioTranscriptionProperties.class));
				assertNotNull(context.getBeansOfType(DashScopeAudioTranscriptionModel.class));
			});

	}

}

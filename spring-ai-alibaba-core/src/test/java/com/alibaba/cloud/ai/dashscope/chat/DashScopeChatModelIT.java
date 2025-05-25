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
package com.alibaba.cloud.ai.dashscope.chat;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.dashscope.DashscopeAiTestConfiguration;
import com.alibaba.cloud.ai.dashscope.chat.tool.MockWeatherService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.model.Media;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DASHSCOPE_API_KEY;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashscopeAiTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = DASHSCOPE_API_KEY, matches = ".+")
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_HTTP_BASE_URL", matches = ".+")
public class DashScopeChatModelIT {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeChatModelIT.class);

	@Autowired
	private ChatModel dashscopeChatModel;

	@Value("classpath:/prompts/system-message.st")
	private Resource systemResource;

	@Test
	void call() {
		ChatResponse response = dashscopeChatModel.call(new Prompt("杭州有哪些美食?"));
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		System.out.printf("content: %s\n", content);
	}

	@Test
	void stream() throws InterruptedException {
		Flux<ChatResponse> response = dashscopeChatModel.stream(new Prompt("杭州有哪些美食?"));

		CountDownLatch cdl = new CountDownLatch(1);
		response.subscribe(data -> {
			System.out.printf("%s", data.getResult().getOutput().getContent());
		}, err -> {
			System.out.printf("err: %s\n", err);
		}, () -> {
			System.out.println("\ndone");
			cdl.countDown();
		});

		cdl.await();
	}

	@Test
	void roleTest() {
		UserMessage userMessage = new UserMessage(
				"Tell me about 3 famous pirates from the Golden Age of Piracy and why they did.");
		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemResource);
		Message systemMessage = systemPromptTemplate.createMessage(Map.of("name", "Bob", "voice", "pirate"));
		Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
		ChatResponse response = dashscopeChatModel.call(prompt);
		assertThat(response.getResults()).hasSize(1);
		assertThat(response.getResults().get(0).getOutput().getContent()).contains("Blackbeard");
	}

	@Test
	void outputParser() {
		DefaultConversionService conversionService = new DefaultConversionService();
		ListOutputConverter outputConverter = new ListOutputConverter(conversionService);

		String format = outputConverter.getFormat();
		String template = """
				List five {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "ice cream flavors", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		org.springframework.ai.chat.model.Generation generation = this.dashscopeChatModel.call(prompt).getResult();

		List<String> list = outputConverter.convert(generation.getOutput().getContent());
		assertThat(list).hasSize(5);

	}

	@Test
	void mapOutputParser() {
		MapOutputConverter mapOutputConverter = new MapOutputConverter();

		String format = mapOutputConverter.getFormat();
		String template = """
				Provide me a List of {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "an array of numbers from 1 to 9 under they key name 'numbers'", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		org.springframework.ai.chat.model.Generation generation = dashscopeChatModel.call(prompt).getResult();
		String generationText = generation.getOutput().getContent().replace("```json", "").replace("```", "");

		Map<String, Object> result = mapOutputConverter.convert(generationText);
		assertThat(result.get("numbers")).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

	}

	@Test
	void beanStreamOutputParserRecords() {

		BeanOutputConverter<ActorsFilmsRecord> outputParser = new BeanOutputConverter<>(ActorsFilmsRecord.class);

		String format = outputParser.getFormat();
		String template = """
				Generate the filmography of 5 movies for Tom Hanks.
				{format}.
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template, Map.of("format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());

		String generationTextFromStream = dashscopeChatModel.stream(prompt)
			.collectList()
			.block()
			.stream()
			.map(ChatResponse::getResults)
			.flatMap(List::stream)
			.map(org.springframework.ai.chat.model.Generation::getOutput)
			.map(AssistantMessage::getContent)
			.collect(Collectors.joining());
		generationTextFromStream = generationTextFromStream.replace("```json", "").replace("```", "");

		ActorsFilmsRecord actorsFilms = outputParser.convert(generationTextFromStream);
		logger.info("" + actorsFilms);
		assertThat(actorsFilms.actor()).isEqualTo("Tom Hanks");
		assertThat(actorsFilms.movies()).hasSize(5);
	}

	record ActorsFilmsRecord(String actor, List<String> movies) {
	}

	@Test
	void functionCallTest() {

		UserMessage userMessage = new UserMessage("What's the weather like in San Francisco?");

		List<Message> messages = new ArrayList<>(List.of(userMessage));

		var promptOptions = DashScopeChatOptions.builder()
			// .withModel(DashScopeApi.dashscopeChatModel.QWEN_MAX.getModel())
			.withFunctionCallbacks(List.of(FunctionCallbackWrapper.builder(new MockWeatherService())
				.withName("getCurrentWeather")
				.withDescription("Get the weather in location")
				.withResponseConverter(MockWeatherService.Response::description)
				.build()))
			.build();

		ChatResponse response = dashscopeChatModel.call(new Prompt(messages, promptOptions));

		logger.info("Response: {}", response);

		assertThat(response.getResult().getOutput().getContent()).containsAnyOf("30.0", "30");
	}

	// @Test
	// void streamFunctionCallTest() {
	//
	// UserMessage userMessage = new UserMessage("What's the weather like Paris?");
	//
	// List<Message> messages = new ArrayList<>(List.of(userMessage));
	//
	// var promptOptions = DashscopeChatOptions.builder()
	// .withFunctionCallbacks(List.of(FunctionCallbackWrapper.builder(new
	// MockWeatherService())
	// .withName("getCurrentWeather")
	// .withDescription("Get the weather in location")
	// .withResponseConverter((response) -> "" + response.temp() + response.unit())
	// .build()))
	// .build();
	//
	// Flux<ChatResponse> response = chatClient.stream(new Prompt(messages,
	// promptOptions));
	//
	// String content = response.collectList()
	// .block()
	// .stream()
	// .map(ChatResponse::getResults)
	// .flatMap(List::stream)
	// .map(Generation::getOutput)
	// .map(AssistantMessage::getContent)
	// .collect(Collectors.joining());
	// logger.info("Response: {}", content);
	//
	// assertThat(content).containsAnyOf("15.0", "15");
	// }

	@Test
	void usageInStream() {
		DefaultConversionService conversionService = new DefaultConversionService();
		ListOutputConverter outputParser = new ListOutputConverter(conversionService);

		String format = outputParser.getFormat();
		String template = """
				List five {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "ice cream flavors", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		List<ChatResponse> responses = this.dashscopeChatModel.stream(prompt).collectList().block();
		ChatResponse response = responses.get(responses.size() - 1);
		assertThat(response.getMetadata().getUsage().getTotalTokens()).isEqualTo(48);
	}

	@Test
	void callMultiModelWithImages() throws MalformedURLException, URISyntaxException {
		List<Media> mediaList = new ArrayList<>();
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL()));
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/tiger.png").toURL()));
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/rabbit.png").toURL()));

		UserMessage message = new UserMessage("这些是什么?", mediaList);
		ChatResponse response = dashscopeChatModel.call(new Prompt(message,
				DashScopeChatOptions.builder().withModel("qwen-vl-max-latest").withMultiModel(true).build()));
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		System.out.printf("content: %s\n", content);
	}

	@Test
	void callMultiModelWithVideo() throws MalformedURLException, URISyntaxException {
		List<Media> mediaList = new ArrayList<>();
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, new URI(
				"https://img.alicdn.com/imgextra/i3/O1CN01K3SgGo1eqmlUgeE9b_!!6000000003923-0-tps-3840-2160.jpg")
			.toURL()));
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, new URI(
				"https://img.alicdn.com/imgextra/i4/O1CN01BjZvwg1Y23CF5qIRB_!!6000000003000-0-tps-3840-2160.jpg")
			.toURL()));
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, new URI(
				"https://img.alicdn.com/imgextra/i4/O1CN01Ib0clU27vTgBdbVLQ_!!6000000007859-0-tps-3840-2160.jpg")
			.toURL()));
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, new URI(
				"https://img.alicdn.com/imgextra/i1/O1CN01aygPLW1s3EXCdSN4X_!!6000000005710-0-tps-3840-2160.jpg")
			.toURL()));

		UserMessage message = new UserMessage("描述这个视频的具体过程", mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.VIDEO);

		ChatResponse response = dashscopeChatModel.call(new Prompt(message,
				DashScopeChatOptions.builder().withModel("qwen-vl-max-latest").withMultiModel(true).build()));
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		System.out.printf("content: %s\n", content);
	}

	@Test
	void callMultiModelWithImageBinary() {
		List<Media> mediaList = new ArrayList<>();
		var imageResource = new ClassPathResource("/multimodel/dog_and_girl.jpeg");
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG, imageResource));

		UserMessage message = new UserMessage("这是什么?", mediaList);
		ChatResponse response = dashscopeChatModel.call(new Prompt(message,
				DashScopeChatOptions.builder().withModel("qwen-vl-max-latest").withMultiModel(true).build()));
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		System.out.printf("content: %s\n", content);
	}

	@Test
	void streamCallMultiModel() throws MalformedURLException, InterruptedException, URISyntaxException {
		List<Media> mediaList = new ArrayList<>();
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL()));
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/tiger.png").toURL()));
		mediaList.add(new Media(MimeTypeUtils.IMAGE_PNG,
				new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/rabbit.png").toURL()));

		UserMessage message = new UserMessage("这些是什么?", mediaList);
		Flux<ChatResponse> response = dashscopeChatModel.stream(new Prompt(message,
				DashScopeChatOptions.builder().withModel("qwen-vl-max-latest").withMultiModel(true).build()));

		CountDownLatch cdl = new CountDownLatch(1);
		response.subscribe(data -> {
			System.out.printf("%s", data.getResult().getOutput().getContent());
		}, err -> {
			System.out.printf("err: %s\n", err);
		}, () -> {
			System.out.println("\ndone");
			cdl.countDown();
		});

		cdl.await();
	}

}

package com.alibaba.cloud.ai.dashscope.chat;

import com.alibaba.cloud.ai.dashscope.DashscopeAiTestConfiguration;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.tool.MockWeatherService;
import com.alibaba.dashscope.aigc.generation.Generation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.model.function.FunctionCallbackWrapper;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.ai.parser.ListOutputParser;
import org.springframework.ai.parser.MapOutputParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashscopeAiTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_HTTP_BASE_URL", matches = ".+")
public class DashScopeChatModelIT {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeChatModelIT.class);

	@Autowired
	private DashScopeChatModel chatModel;

	@Value("classpath:/prompts/system-message.st")
	private Resource systemResource;

	@Test
	void call() {
		ChatResponse response = chatModel.call(new Prompt("杭州有哪些美食?"));
		String content = response.getResult().getOutput().getContent();
		Assertions.assertNotNull(content);

		System.out.printf("content: %s\n", content);
	}

	@Test
	void stream() throws InterruptedException {
		Flux<ChatResponse> response = chatModel.stream(new Prompt("杭州有哪些美食?"));

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
		ChatResponse response = chatModel.call(prompt);
		assertThat(response.getResults()).hasSize(1);
		assertThat(response.getResults().get(0).getOutput().getContent()).contains("Blackbeard");
	}

	@Test
	void outputParser() {
		DefaultConversionService conversionService = new DefaultConversionService();
		ListOutputParser outputParser = new ListOutputParser(conversionService);

		String format = outputParser.getFormat();
		String template = """
				List five {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "ice cream flavors", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		org.springframework.ai.chat.model.Generation generation = this.chatModel.call(prompt).getResult();

		List<String> list = outputParser.parse(generation.getOutput().getContent());
		assertThat(list).hasSize(5);

	}

	@Test
	void mapOutputParser() {
		MapOutputParser outputParser = new MapOutputParser();

		String format = outputParser.getFormat();
		String template = """
				Provide me a List of {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "an array of numbers from 1 to 9 under they key name 'numbers'", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		org.springframework.ai.chat.model.Generation generation = chatModel.call(prompt).getResult();
		String generationText = generation.getOutput().getContent().replace("```json", "").replace("```", "");

		Map<String, Object> result = outputParser.parse(generationText);
		assertThat(result.get("numbers")).isEqualTo(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9));

	}

	@Test
	void beanStreamOutputParserRecords() {

		BeanOutputParser<ActorsFilmsRecord> outputParser = new BeanOutputParser<>(ActorsFilmsRecord.class);

		String format = outputParser.getFormat();
		String template = """
				Generate the filmography of 5 movies for Tom Hanks.
				{format}.
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template, Map.of("format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());

		String generationTextFromStream = chatModel.stream(prompt)
			.collectList()
			.block()
			.stream()
			.map(ChatResponse::getResults)
			.flatMap(List::stream)
			.map(org.springframework.ai.chat.model.Generation::getOutput)
			.map(AssistantMessage::getContent)
			.collect(Collectors.joining());
		generationTextFromStream = generationTextFromStream.replace("```json", "").replace("```", "");

		ActorsFilmsRecord actorsFilms = outputParser.parse(generationTextFromStream);
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
			// .withModel(DashScopeApi.ChatModel.QWEN_MAX.getModel())
			.withFunctionCallbacks(List.of(FunctionCallbackWrapper.builder(new MockWeatherService())
				.withName("getCurrentWeather")
				.withDescription("Get the weather in location")
				.withResponseConverter(MockWeatherService.Response::description)
				.build()))
			.build();

		ChatResponse response = chatModel.call(new Prompt(messages, promptOptions));

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
		ListOutputParser outputParser = new ListOutputParser(conversionService);

		String format = outputParser.getFormat();
		String template = """
				List five {subject}
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(template,
				Map.of("subject", "ice cream flavors", "format", format));
		Prompt prompt = new Prompt(promptTemplate.createMessage());
		List<ChatResponse> responses = this.chatModel.stream(prompt).collectList().block();
		ChatResponse response = responses.get(responses.size() - 1);
		assertThat(response.getMetadata().getUsage().getTotalTokens()).isEqualTo(48);
	}

}

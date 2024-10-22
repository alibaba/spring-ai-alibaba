package com.alibaba.cloud.ai.example.prompt;

import java.util.Map;

import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplate;
import com.alibaba.cloud.ai.prompt.ConfigurablePromptTemplateFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class PromptTemplateController {

	private final ChatClient chatClient;
	
	private final ConfigurablePromptTemplateFactory configurablePromptTemplateFactory;
	
	
	@Value("classpath:/prompts/joke-prompt.st")
	private Resource jokeResource;
	
	@Autowired
	public PromptTemplateController(ChatClient.Builder builder,
			ConfigurablePromptTemplateFactory configurablePromptTemplateFactory) {
		this.chatClient = builder.build();
		this.configurablePromptTemplateFactory = configurablePromptTemplateFactory;
	}
	
	@GetMapping("/prompt")
	public AssistantMessage completion(@RequestParam(value = "adjective", defaultValue = "funny") String adjective,
			@RequestParam(value = "topic", defaultValue = "cows") String topic) {
		PromptTemplate promptTemplate = new PromptTemplate(jokeResource);
		Prompt prompt = promptTemplate.create(Map.of("adjective", adjective, "topic", topic));
		return chatClient.prompt(prompt).call().chatResponse().getResult().getOutput();
	}
	
	/**
	 * nacos template config [{"name:"test-template","template:"please list the most famous books by this {author}."}]
	 */
	
	@GetMapping("/prompt-template")
	public AssistantMessage generate(@RequestParam(value = "author") String author) {
		ConfigurablePromptTemplate template = configurablePromptTemplateFactory.create("test-template",
				"please list the three most famous books by this {author}.");
		Prompt prompt;
		if (StringUtils.hasText(author)) {
			prompt = template.create(Map.of("author", author));
		} else {
			prompt = template.create();
		}
		return chatClient.prompt(prompt).call().chatResponse().getResult().getOutput();
	}

}

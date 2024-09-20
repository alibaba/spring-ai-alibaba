package com.alibaba.cloud.ai.example.outparser;

import java.util.Map;

import com.alibaba.cloud.ai.example.outparser.entity.ActorsFilms;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.parser.BeanOutputParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class OutputParserController {

	private static final Logger logger = LoggerFactory.getLogger(OutputParserController.class);

	private final ChatClient chatClient;

	@Autowired
	public OutputParserController(ChatClient.Builder builder) {
		this.chatClient = builder.build();
	}

	@GetMapping("/output")
	public ActorsFilms generate(@RequestParam(value = "actor", defaultValue = "Jeff Bridges") String actor) {
		var outputParser = new BeanOutputParser<>(ActorsFilms.class);

		String format = outputParser.getFormat();
		logger.info("format: " + format);
		String userMessage = """
				Generate the filmography for the actor {actor}.
				{format}
				""";
		PromptTemplate promptTemplate = new PromptTemplate(userMessage, Map.of("actor", actor, "format", format));
		Prompt prompt = promptTemplate.create();
		Generation generation = chatClient.prompt(prompt).call().chatResponse().getResult();

		return outputParser.parse(generation.getOutput().getContent());
	}

}

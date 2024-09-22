package com.alibaba.cloud.ai.example.outparser.controller;

import java.util.List;
import java.util.Objects;

import com.alibaba.cloud.ai.example.outparser.entity.ActorsFilms;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class OutputParserController {

	private final ChatClient chatClient;

	private final ChatModel chatModel;

	@Autowired
	public OutputParserController(ChatClient.Builder builder, ChatModel chatModel) {
		this.chatClient = builder.build();
		this.chatModel = chatModel;
	}

	@GetMapping("/output/simple")
	public ActorsFilms generate(@RequestParam(value = "actor", defaultValue = "Jeff Bridges") String actor) {

		/**
		 * Low API: chatModel API
		 BeanOutputConverter<ActorsFilms> beanOutputConverter =
		 new BeanOutputConverter<>(ActorsFilms.class);

		 String template = """
		 Generate the filmography of 5 movies for {actor}.
		 {format}
		 """;
		 return beanOutputConverter.convert(
		 chatModel.call(
		 new PromptTemplate(
		 template,
		 Map.of("actor", "Tom Hanks", "format", beanOutputConverter.getFormat())
		 ).create()).getResult().getOutput().getContent());
		 */

		// ChatClient API
		return chatClient.prompt()
				.user(u -> u.text("Generate the filmography of 5 movies for {actor}.")
						.param("actor", "Tom Hanks"))
				.call()
				.entity(ActorsFilms.class);
	}

	@GetMapping("/output/stream")
	public List<ActorsFilms> generateStream(@RequestParam(value = "actor", defaultValue = "Jeff Bridges") String actor) {

		var converter = new BeanOutputConverter<>(new ParameterizedTypeReference<List<ActorsFilms>>() { });

		Flux<String> flux = this.chatClient.prompt()
				.user(u -> u.text("""
								  Generate the filmography for a random actor.
								  {format}
								""")
						.param("format", converter.getFormat()))
				.stream()
				.content();

		return converter.convert(String.join("", Objects.requireNonNull(flux.collectList().block())));
	}

}

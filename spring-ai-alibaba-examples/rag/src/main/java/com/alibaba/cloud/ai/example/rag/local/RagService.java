package com.alibaba.cloud.ai.example.rag.local;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

public class RagService {

	private static final Logger logger = LoggerFactory.getLogger(RagService.class);

	@Value("classpath:/data/bikes.json")
	private Resource bikesResource;

	@Value("classpath:/prompts/system-qa.st")
	private Resource systemBikePrompt;

	private final ChatClient chatClient;

	private final EmbeddingModel embeddingModel;

	public RagService(ChatClient chatClient, EmbeddingModel embeddingModel) {
		this.chatClient = chatClient;
		this.embeddingModel = embeddingModel;
	}

	public AssistantMessage retrieve(String message) {

		// Step 1 - Load JSON document as Documents

		logger.info("Loading JSON as Documents");
		JsonReader jsonReader = new JsonReader(bikesResource, "name", "price", "shortDescription", "description");
		List<Document> documents = jsonReader.get();
		logger.info("Loading JSON as Documents");

		// Step 2 - Create embeddings and save to vector store

		logger.info("Creating Embeddings...");
		VectorStore vectorStore = new SimpleVectorStore(embeddingModel);
		vectorStore.add(documents);
		logger.info("Embeddings created.");

		// Step 3 retrieve related documents to query
		logger.info("Retrieving relevant documents");
		List<Document> similarDocuments = vectorStore.similaritySearch(message);
		logger.info(String.format("Found %s relevant documents.", similarDocuments.size()));

		// Step 4 Embed documents into SystemMessage with the `system-qa.st` prompt
		// template
		Message systemMessage = getSystemMessage(similarDocuments);
		UserMessage userMessage = new UserMessage(message);

		// Step 4 - Ask the AI model

		logger.info("Asking AI model to reply to question.");
		Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
		logger.info(prompt.toString());

		ChatResponse chatResponse = chatClient.prompt(prompt).call().chatResponse();
		logger.info("AI responded.");

		logger.info(chatResponse.getResult().getOutput().getContent());
		return chatResponse.getResult().getOutput();
	}

	private Message getSystemMessage(List<Document> similarDocuments) {

		String documents = similarDocuments.stream().map(entry -> entry.getContent()).collect(Collectors.joining("\n"));
		SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemBikePrompt);
		Message systemMessage = systemPromptTemplate.createMessage(Map.of("documents", documents));
		return systemMessage;

	}

}

/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.dotprompt.DotPrompt;
import com.alibaba.cloud.ai.dotprompt.DotPromptService;
import com.alibaba.cloud.ai.dotprompt.DotPromptTemplate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * REST API controller for managing DotPrompt templates.
 */
@CrossOrigin
@RestController
@RequestMapping("studio/api/dotprompt")
public class DotPromptAPIController {

	private final DotPromptService dotPromptService;

	private final DotPromptTemplate dotPromptTemplate;

	private final ChatClient chatClient;

	public DotPromptAPIController(DotPromptService dotPromptService, DotPromptTemplate dotPromptTemplate,
								  ChatClient.Builder builder) {
		this.dotPromptService = dotPromptService;
		this.dotPromptTemplate = dotPromptTemplate;
		this.chatClient = builder.build();
	}

	/**
	 * Get a list of all available prompt names.
	 * @return List of prompt names
	 */
	@GetMapping("/list")
	public ResponseEntity<List<String>> listPrompts() throws IOException {
		return ResponseEntity.ok(dotPromptService.getPromptNames());
	}

	/**
	 * Get details of a specific prompt by name.
	 * @param promptName Name of the prompt to retrieve
	 * @return The prompt details
	 */
	@GetMapping("/{promptName}")
	public ResponseEntity<DotPrompt> getPrompt(@PathVariable String promptName) throws IOException {
		return ResponseEntity.ok(dotPromptService.getPrompt(promptName));
	}

	/**
	 * Chat using a specific prompt template.
	 * @param request The chat request containing prompt name and variables
	 * @return The chat response
	 */
	@PostMapping("/chat")
	public ResponseEntity<ChatResponse> chat(@RequestBody DotPromptChatRequest request) throws IOException {
		Assert.hasText(request.getPromptName(), "promptName must not be empty");

		DotPromptTemplate template = dotPromptTemplate.withPrompt(request.getPromptName());

		// Override model if specified
		if (request.getModel() != null) {
			template = template.withModel(request.getModel());
		}

		// Override config if specified
		if (request.getConfig() != null) {
			template = template.withConfig(request.getConfig());
		}

		// Create message with variables
		return ResponseEntity.ok(chatClient.prompt(template.create(request.getVariables())).call().chatResponse());
	}

}

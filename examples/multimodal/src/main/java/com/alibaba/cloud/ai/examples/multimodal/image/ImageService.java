/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multimodal.image;

import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.util.List;

/**
 * Service for image understanding (vision) scenarios.
 */
@Service
public class ImageService {

	private static final Logger log = LoggerFactory.getLogger(ImageService.class);

	private final ChatModel chatModel;

	private final ReactAgent visionAgent;

	public ImageService(ChatModel chatModel, @Qualifier("visionAgent") ReactAgent visionAgent) {
		this.chatModel = chatModel;
		this.visionAgent = visionAgent;
	}

	/**
	 * Describe an image from a public URL using ChatModel directly.
	 */
	public String describeImageFromUrl(String imageUrl, String question) {
		UserMessage userMessage = UserMessage.builder()
				.text(question)
				.media(List.of(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl))))
				.build();

		ChatResponse response = chatModel.call(new Prompt(userMessage));
		String result = response.getResult().getOutput().getText();
		log.debug("Image description from URL: {}", result);
		return result;
	}

	/**
	 * Describe an image from a local resource (file or classpath).
	 */
	public String describeImageFromResource(Resource imageResource, String question) {
		return describeImageFromResource(imageResource, question, MimeTypeUtils.IMAGE_PNG);
	}

	/**
	 * Describe an image from a local resource with explicit MIME type (e.g. for uploaded files).
	 */
	public String describeImageFromResource(Resource imageResource, String question, MimeType mimeType) {
		UserMessage userMessage = UserMessage.builder()
				.text(question)
				.media(new Media(mimeType, imageResource))
				.build();

		ChatResponse response = chatModel.call(new Prompt(userMessage));
		String result = response.getResult().getOutput().getText();
		log.debug("Image description from resource: {}", result);
		return result;
	}

	/**
	 * Use vision agent with multimodal input - pass UserMessage with image media.
	 */
	public AssistantMessage visionAgentCall(UserMessage userMessage) throws GraphRunnerException {
		return visionAgent.call(userMessage);
	}

	/**
	 * Convenience: describe image from URL using the vision agent.
	 */
	public String visionAgentDescribeFromUrl(String imageUrl, String question) throws GraphRunnerException {
		UserMessage userMessage = UserMessage.builder()
				.text(question)
				.media(List.of(new Media(MimeTypeUtils.IMAGE_PNG, URI.create(imageUrl))))
				.build();
		AssistantMessage response = visionAgent.call(userMessage);
		return response.getText();
	}
}

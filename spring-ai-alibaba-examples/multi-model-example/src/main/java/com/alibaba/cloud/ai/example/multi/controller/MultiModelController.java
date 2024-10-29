/*
 * Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.example.multi.controller;

import java.net.URI;
import java.util.List;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.chat.MessageFormat;
import com.alibaba.cloud.ai.example.multi.helper.FrameExtraHelper;
import jakarta.annotation.Resource;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@RestController
@RequestMapping("/ai/multi")
public class MultiModelController {

	@Resource
	private ChatModel chatModel;

	@Resource
	private ResourceLoader resourceLoader;

	private static final String DEFAULT_PROMPT = "这些是什么？";

	private static final String DEFAULT_MODEL = "qwen-vl-max-latest";

	@GetMapping("/image")
	public String image(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) throws Exception {

		List<Media> mediaList = List.of(
				new Media(
						MimeTypeUtils.IMAGE_PNG,
						new URI("https://dashscope.oss-cn-beijing.aliyuncs.com/images/dog_and_girl.jpeg").toURL()
				)
		);

		UserMessage message = new UserMessage(prompt, mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		ChatResponse response = chatModel.call(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		);

		return response.getResult().getOutput().getContent();
	}

	@GetMapping("/video")
	public String video(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) {

		List<Media> mediaList = FrameExtraHelper.createMediaList(10);

		UserMessage message = new UserMessage(prompt, mediaList);
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.VIDEO);

		ChatResponse response = chatModel.call(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		);

		return response.getResult().getOutput().getContent();
	}

	@GetMapping("/image/bin")
	public String imagesBinary(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) {

		UserMessage message = new UserMessage(
				prompt,
				new Media(
						MimeTypeUtils.IMAGE_PNG,
						resourceLoader.getResource("classpath:/multimodel/dog_and_girl.jpeg")
				));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		ChatResponse response = chatModel.call(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		);

		return response.getResult().getOutput().getContent();
	}

	@GetMapping("/stream/image")
	public String streamImage(
			@RequestParam(value = "prompt", required = false, defaultValue = DEFAULT_PROMPT)
			String prompt
	) {

		UserMessage message = new UserMessage(
				prompt,
				new Media(
						MimeTypeUtils.IMAGE_PNG,
						resourceLoader.getResource("classpath:/multimodel/dog_and_girl.jpeg")
				));
		message.getMetadata().put(DashScopeChatModel.MESSAGE_FORMAT, MessageFormat.IMAGE);

		List<ChatResponse> response = chatModel.stream(
				new Prompt(
						message,
						DashScopeChatOptions.builder()
								.withModel(DEFAULT_MODEL)
								.withMultiModel(true)
								.build()
				)
		).collectList().block();

		StringBuilder result = new StringBuilder();
		if (response != null) {
			for (ChatResponse chatResponse : response) {
				String outputContent = chatResponse.getResult().getOutput().getContent();
				result.append(outputContent);
			}
		}

		return result.toString();
	}

}

/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service.impl;

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.exception.NotFoundException;
import com.alibaba.cloud.ai.model.ChatModel;
import com.alibaba.cloud.ai.param.ModelRunActionParam;
import com.alibaba.cloud.ai.service.ChatModelDelegate;
import com.alibaba.cloud.ai.utils.SpringApplicationUtil;
import com.alibaba.cloud.ai.vo.ActionResult;
import com.alibaba.cloud.ai.vo.ChatModelRunResult;
import com.alibaba.cloud.ai.vo.TelemetryResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.tracing.Tracer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.image.ImageMessage;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
public class ChatModelDelegateImpl implements ChatModelDelegate {

	private final Tracer tracer;

	private final ObjectMapper objectMapper;

	public ChatModelDelegateImpl(Tracer tracer) {
		this.tracer = tracer;
		this.objectMapper = new ObjectMapper();
	}

	@Override
	public List<ChatModel> list() {
		List<ChatModel> res = new ArrayList<>();

		// ChatModel
		Map<String, org.springframework.ai.chat.model.ChatModel> chatModelMap = SpringApplicationUtil
			.getBeans(org.springframework.ai.chat.model.ChatModel.class);
		for (Map.Entry<String, org.springframework.ai.chat.model.ChatModel> entry : chatModelMap.entrySet()) {
			org.springframework.ai.chat.model.ChatModel chatModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), chatModel.getClass());

			ChatModel model = ChatModel.builder()
				.name(entry.getKey())
				.model(chatModel.getDefaultOptions().getModel())
				.modelType(ModelType.CHAT)
				.build();

			if (chatModel.getClass() == DashScopeChatModel.class) {
				DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
				model.setChatOptions(dashScopeChatModel.getDashScopeChatOptions());
			}

			res.add(model);
		}

		// ImageModel
		Map<String, ImageModel> imageModelMap = SpringApplicationUtil.getBeans(ImageModel.class);
		for (Map.Entry<String, ImageModel> entry : imageModelMap.entrySet()) {
			ImageModel imageModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), imageModel.getClass());
			ChatModel model = ChatModel.builder().name(entry.getKey()).modelType(ModelType.IMAGE).build();

			if (imageModel.getClass() == DashScopeImageModel.class) {
				DashScopeImageModel dashScopeImageModel = (DashScopeImageModel) imageModel;
				model.setModel(dashScopeImageModel.getOptions().getModel());
				model.setImageOptions(dashScopeImageModel.getOptions());
			}
			res.add(model);
		}

		// TODO AudioModel
		return res;
	}

	@Override
	public ChatModel getByModelName(String modelName) {
		org.springframework.ai.chat.model.ChatModel chatModel = getChatModel(modelName);
		if (chatModel != null) {
			ChatModel model = ChatModel.builder()
				.name(modelName)
				.model(chatModel.getDefaultOptions().getModel())
				.modelType(ModelType.CHAT)
				.build();
			if (chatModel.getClass() == DashScopeChatModel.class) {
				DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
				model.setChatOptions(dashScopeChatModel.getDashScopeChatOptions());
			}
			return model;
		}

		ImageModel imageModel = getImageModel(modelName);
		if (imageModel != null) {
			ChatModel model = ChatModel.builder().name(modelName).modelType(ModelType.IMAGE).build();
			if (imageModel.getClass().equals(DashScopeImageModel.class)) {
				DashScopeImageModel dashScopeImageModel = (DashScopeImageModel) imageModel;
				model.setModel(dashScopeImageModel.getOptions().getModel());
				model.setImageOptions(dashScopeImageModel.getOptions());
			}
			return model;
		}

		// TODO AudioModel

		log.error("can not find by bean name:{}", modelName);
		throw new NotFoundException();
	}

	@Override
	public ChatModelRunResult run(ModelRunActionParam runActionParam) {
		String key = runActionParam.getKey();
		String input = runActionParam.getInput();
		DashScopeChatOptions chatOptions = runActionParam.getChatOptions();
		String prompt = runActionParam.getPrompt();

		org.springframework.ai.chat.model.ChatModel chatModel = getChatModel(key);
		if (chatModel != null) {
			if (chatModel.getClass() == DashScopeChatModel.class) {
				DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
				if (chatOptions != null) {
					try {
						log.info("set chat options, {}", objectMapper.writeValueAsString(chatOptions));
					}
					catch (JsonProcessingException e) {
						throw new RuntimeException("Failed to serialize JSON", e);
					}
					dashScopeChatModel.setDashScopeChatOptions(chatOptions);
				}
			}
			List<Message> messages = new ArrayList<>();
			if (StringUtils.hasText(prompt)) {
				Message systemMessage = new SystemMessage(prompt);
				messages.add(systemMessage);
			}
			Message userMessage = new UserMessage(input);
			messages.add(userMessage);
			ChatResponse response = chatModel.call(new Prompt(messages));
			String resp = response.getResult().getOutput().getText();
			return ChatModelRunResult.builder()
				.input(runActionParam)
				.result(ActionResult.builder().Response(resp).build())
				.telemetry(TelemetryResult.builder().traceId(tracer.currentSpan().context().traceId()).build())
				.build();
		}

		log.error("can not find by bean name:{}", key);
		throw new NotFoundException();
	}

	@Override
	public String runImageGenTask(ModelRunActionParam runActionParam) {
		String key = runActionParam.getKey();
		String input = runActionParam.getInput();
		DashScopeImageOptions imageOptions = runActionParam.getImageOptions();
		String prompt = runActionParam.getPrompt();

		ImageModel imageModel = getImageModel(key);
		if (imageModel != null) {
			if (imageModel.getClass() == DashScopeImageModel.class) {
				DashScopeImageModel dashScopeImageModel = (DashScopeImageModel) imageModel;
				if (imageOptions != null) {
					try {
						log.info("set image options, {}", objectMapper.writeValueAsString(imageOptions));
					}
					catch (JsonProcessingException e) {
						throw new RuntimeException("Failed to serialize JSON", e);
					}
					dashScopeImageModel.setOptions(imageOptions);
				}
				else {
					imageOptions = DashScopeImageOptions.builder()
						.withModel(dashScopeImageModel.getOptions().getModel())
						.build();
				}
			}
			if (imageOptions == null) {
				imageOptions = DashScopeImageOptions.builder()
					.withModel(DashScopeImageApi.ImageModel.WANX_V1.getValue())
					.build();
			}
			ImageOptions options = ImageOptionsBuilder.builder()
				.model(imageOptions.getModel())
				.N(imageOptions.getN())
				.width(imageOptions.getWidth())
				.height(imageOptions.getHeight())
				.style(imageOptions.getStyle())
				.build();
			List<ImageMessage> messages = new ArrayList<>();
			if (StringUtils.hasText(prompt)) {
				ImageMessage systemMessage = new ImageMessage(prompt);
				messages.add(systemMessage);
			}
			ImageMessage userMessage = new ImageMessage(input);
			messages.add(userMessage);

			ImageResponse imageResponse = imageModel.call(new ImagePrompt(messages, options));
			return imageResponse.getResult().getOutput().getUrl();
		}

		log.error("can not find by bean name:{}", key);
		throw new NotFoundException();
	}

	@Override
	public ChatModelRunResult runImageGenTaskAndGetUrl(ModelRunActionParam modelRunActionParam) {
		String imageUrl = runImageGenTask(modelRunActionParam);
		return ChatModelRunResult.builder()
			.input(modelRunActionParam)
			.result(ActionResult.builder().Response(imageUrl).build())
			.telemetry(TelemetryResult.builder().traceId(tracer.currentSpan().context().traceId()).build())
			.build();
	}

	@Override
	public List<String> listModelNames(ModelType modelType) {
		List<String> res = new ArrayList<>();
		if (modelType == ModelType.CHAT) {
			DashScopeApi.ChatModel[] values = DashScopeApi.ChatModel.values();
			for (DashScopeApi.ChatModel value : values) {
				res.add(value.getModel());
			}
		}
		else if (modelType == ModelType.IMAGE) {
			DashScopeImageApi.ImageModel[] values = DashScopeImageApi.ImageModel.values();
			for (DashScopeImageApi.ImageModel value : values) {
				res.add(value.getValue());
			}
		}

		return res;
	}

	private org.springframework.ai.chat.model.ChatModel getChatModel(String modelName) {
		Map<String, org.springframework.ai.chat.model.ChatModel> chatModelMap = SpringApplicationUtil
			.getBeans(org.springframework.ai.chat.model.ChatModel.class);
		for (Map.Entry<String, org.springframework.ai.chat.model.ChatModel> entry : chatModelMap.entrySet()) {
			org.springframework.ai.chat.model.ChatModel chatModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), chatModel.getClass());

			if (entry.getKey().equals(modelName)) {
				return chatModel;
			}
		}

		return null;
	}

	private ImageModel getImageModel(String modelName) {
		Map<String, ImageModel> imageModelMap = SpringApplicationUtil.getBeans(ImageModel.class);
		for (Map.Entry<String, ImageModel> entry : imageModelMap.entrySet()) {
			ImageModel imageModel = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), imageModel.getClass());

			if (entry.getKey().equals(modelName)) {
				return imageModel;
			}
		}

		return null;
	}

}

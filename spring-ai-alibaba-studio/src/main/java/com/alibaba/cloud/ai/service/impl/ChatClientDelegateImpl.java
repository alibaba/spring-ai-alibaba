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
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.exception.ServiceInternalException;
import com.alibaba.cloud.ai.model.ChatClient;
import com.alibaba.cloud.ai.param.ClientRunActionParam;
import com.alibaba.cloud.ai.service.ChatClientDelegate;
import com.alibaba.cloud.ai.utils.SpringApplicationUtil;
import com.alibaba.cloud.ai.vo.ActionResult;
import com.alibaba.cloud.ai.vo.ChatClientRunResult;
import com.alibaba.cloud.ai.vo.TelemetryResult;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.tracing.Tracer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.DefaultChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY;
import static org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor.CHAT_MEMORY_RETRIEVE_SIZE_KEY;

@Service
@Slf4j
public class ChatClientDelegateImpl implements ChatClientDelegate {

	private static final int CHAT_MEMORY_RETRIEVE_SIZE = 100;

	private final Tracer tracer;

	public ChatClientDelegateImpl(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public List<ChatClient> list() {
		List<ChatClient> res = new ArrayList<>();
		Map<String, org.springframework.ai.chat.client.ChatClient> chatClientMap = SpringApplicationUtil
			.getBeans(org.springframework.ai.chat.client.ChatClient.class);
		for (Map.Entry<String, org.springframework.ai.chat.client.ChatClient> entry : chatClientMap.entrySet()) {
			org.springframework.ai.chat.client.ChatClient chatClient = entry.getValue();
			log.info("bean name:{}, bean Class:{}", entry.getKey(), chatClient.getClass());
			ChatClient client = getChatClientVo(chatClient, entry.getKey());

			res.add(client);
		}
		return res;
	}

	@Override
	public ChatClient get(String clientName) {
		org.springframework.ai.chat.client.ChatClient chatClient = getChatClientByBeanName(clientName);
		return getChatClientVo(chatClient, clientName);
	}

	@Override
	public ChatClientRunResult run(ClientRunActionParam runActionParam) {
		String key = runActionParam.getKey();
		String input = runActionParam.getInput();
		String prompt = runActionParam.getPrompt();
		DashScopeChatOptions chatOptions = runActionParam.getChatOptions();

		org.springframework.ai.chat.client.ChatClient chatClient = getChatClientByBeanName(key);

		org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec clientRequestSpec = chatClient.prompt();
		if (StringUtils.hasText(prompt)) {
			clientRequestSpec.system(prompt);
		}
		if (chatOptions != null) {
			clientRequestSpec.options(chatOptions);
		}

		String chatID = runActionParam.getChatID();
		if (!StringUtils.hasText(chatID)) {
			// 新会话
			chatID = UUID.randomUUID().toString();
		}
		String finalChatID = chatID;
		clientRequestSpec.advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, finalChatID)
			.param(CHAT_MEMORY_RETRIEVE_SIZE_KEY, CHAT_MEMORY_RETRIEVE_SIZE));

		String resp = clientRequestSpec.user(input).call().content();
		return ChatClientRunResult.builder()
			.input(runActionParam)
			.result(ActionResult.builder().Response(resp).build())
			.ChatID(chatID)
			.telemetry(TelemetryResult.builder().traceId(tracer.currentSpan().context().traceId()).build())
			.build();
	}

	private org.springframework.ai.chat.client.ChatClient getChatClientByBeanName(String clientName) {
		return SpringApplicationUtil.getBean(clientName, org.springframework.ai.chat.client.ChatClient.class);
	}

	private ChatClient getChatClientVo(org.springframework.ai.chat.client.ChatClient chatClient, String clientName) {
		ChatClient client = ChatClient.builder().name(clientName).build();
		if (chatClient.getClass() == DefaultChatClient.class) {
			DefaultChatClient defaultClient = (DefaultChatClient) chatClient;

			Field field = ReflectionUtils.findField(DefaultChatClient.class, "defaultChatClientRequest");
			if (field != null) {
				ReflectionUtils.makeAccessible(field);
				try {
					DefaultChatClient.DefaultChatClientRequestSpec defaultChatClientRequest = (DefaultChatClient.DefaultChatClientRequestSpec) field
						.get(defaultClient);

					client.setDefaultSystemText(defaultChatClientRequest.getSystemText());
					client.setDefaultSystemParams(defaultChatClientRequest.getSystemParams());
					client.setChatOptions(defaultChatClientRequest.getChatOptions());
					client.setAdvisors(defaultChatClientRequest.getAdvisors());
					// todo 扩展其他项

					// 获取是否开启memory
					for (Advisor advisor : defaultChatClientRequest.getAdvisors()) {
						try {
							Class<?> clazz = advisor.getClass();
							client.setIsMemoryEnabled(AbstractChatMemoryAdvisor.class.isAssignableFrom(clazz));
						}
						catch (Exception e) {
							client.setIsMemoryEnabled(false);
						}
					}

					// 获取chatModel并设置
					// 目前仅支持 ModelType.CHAT 类型，暂不支持其他类型的Model
					Field chatModelField = ReflectionUtils
						.findField(DefaultChatClient.DefaultChatClientRequestSpec.class, "chatModel");
					if (chatModelField != null) {
						ReflectionUtils.makeAccessible(chatModelField);
						try {
							ChatModel chatModel = (ChatModel) chatModelField.get(defaultChatClientRequest);
							com.alibaba.cloud.ai.model.ChatModel model = com.alibaba.cloud.ai.model.ChatModel.builder()
								.name("chatModel")
								.model(chatModel.getDefaultOptions().getModel())
								.modelType(ModelType.CHAT)
								.build();
							if (chatModel.getClass() == DashScopeChatModel.class) {
								DashScopeChatModel dashScopeChatModel = (DashScopeChatModel) chatModel;
								model.setChatOptions(dashScopeChatModel.getDashScopeChatOptions());
							}
							client.setChatModel(model);
						}
						catch (IllegalAccessException e) {
							log.error("handle ChatModel error", e);
						}
					}
				}
				catch (IllegalAccessException e) {
					log.error("handle DefaultChatClient error", e);
					throw new ServiceInternalException(e.getMessage());
				}
			}
		}
		// todo 扩展spring-ai-alibaba ChatClient实现

		return client;
	}

}

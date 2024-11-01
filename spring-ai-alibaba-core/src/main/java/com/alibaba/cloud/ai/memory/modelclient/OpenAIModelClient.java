package com.alibaba.cloud.ai.memory.modelclient;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.enums.RoleTypeEnum;
import com.alibaba.cloud.ai.memory.handler.ModelHandler;
import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Title openAI client.<br>
 * Description used to interact with openai models.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class OpenAIModelClient implements ModelHandler {

	@Autowired
	private ChatMemoryProperties memoryProperties;

	@Override
	public ChatMessage getResponse(List<ChatMessage> messages, String conversationId, String question,
			ChatMemoryProperties properties) {

		OpenAiApi openAiApi = new OpenAiApi(memoryProperties.getApiKey());
		if (StringUtils.isNotBlank(memoryProperties.getTransitUrl())) {
			openAiApi = new OpenAiApi(memoryProperties.getTransitUrl(), memoryProperties.getApiKey());
		}
		// 转换用户对话历史
		List<OpenAiApi.ChatCompletionMessage> openAImessageList = new ArrayList<>();
		messages.forEach(item -> {
			OpenAiApi.ChatCompletionMessage openAImessage = null;
			if (item.getRole().equals(RoleTypeEnum.USER.getRoleName())) {
				openAImessage = new OpenAiApi.ChatCompletionMessage(item.getContent(),
						OpenAiApi.ChatCompletionMessage.Role.USER);
			}
			else if (item.getRole().equals(RoleTypeEnum.ASSISTANT.getRoleName())) {
				openAImessage = new OpenAiApi.ChatCompletionMessage(item.getContent(),
						OpenAiApi.ChatCompletionMessage.Role.ASSISTANT);
			}
			openAImessageList.add(openAImessage);
		});
		// problems with adding users
		OpenAiApi.ChatCompletionMessage chatCompletionMessage = new OpenAiApi.ChatCompletionMessage(question,
				OpenAiApi.ChatCompletionMessage.Role.USER);
		openAImessageList.add(chatCompletionMessage);
		// add system prompt words
		if (memoryProperties.isIncludeSystemPrompt()) {
			String promptStr = Optional.ofNullable(memoryProperties.getPrompt())
				.filter(StringUtils::isNotBlank)
				.orElseThrow(() -> new IllegalArgumentException("Prompt cannot be empty."));
			OpenAiApi.ChatCompletionMessage promptCompletionMessage = new OpenAiApi.ChatCompletionMessage(promptStr,
					OpenAiApi.ChatCompletionMessage.Role.SYSTEM);
			openAImessageList.add(0, promptCompletionMessage);
		}
		// processing response data
		ResponseEntity<OpenAiApi.ChatCompletion> response = openAiApi
			.chatCompletionEntity(new OpenAiApi.ChatCompletionRequest(openAImessageList,
					memoryProperties.getModelName(), (double) memoryProperties.getTemperature(), false));
		OpenAiApi.ChatCompletion chatCompletion;
		if (response.getStatusCode() == HttpStatus.OK) {
			chatCompletion = response.getBody();
		}
		else {
			throw new RuntimeException("The openAI api call failed! :" + JSON.toJSONString(response));
		}

		ChatMessage chatMessage = null;
		if (Objects.nonNull(chatCompletion) && CollUtil.isNotEmpty(chatCompletion.choices())) {
			OpenAiApi.ChatCompletion.Choice choice = chatCompletion.choices().get(0);
			OpenAiApi.Usage usage = chatCompletion.usage();
			String content = choice.message().content();
			chatMessage = ChatMessage.builder()
				.role(RoleTypeEnum.ASSISTANT.getRoleName())
				.content(content)
				.inputTokens(usage.promptTokens())
				.outputTokens(usage.completionTokens())
				.totalTokens(usage.totalTokens())
				.build();
			return chatMessage;
		}
		return chatMessage;
	}

}
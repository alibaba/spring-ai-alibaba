package com.alibaba.cloud.ai.memory.modelclient;

import com.alibaba.cloud.ai.memory.enums.RoleTypeEnum;
import com.alibaba.cloud.ai.memory.handler.ModelHandler;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Title qwen client.<br>
 * Description used to interact with qwen models.<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class QwenModelClient implements ModelHandler {

	private static final Generation gen = new Generation();

	@Override
	public ChatMessage getResponse(List<ChatMessage> messages, String conversationId, String question,
			ChatMemoryProperties properties) throws NoApiKeyException, InputRequiredException {
		// 创建生成参数并调用 API
		GenerationParam param = createGenerationParam(messages, properties.getApiKey(),
				properties.isIncludeSystemPrompt(), properties.getPrompt(), properties.getModelName(),
				properties.getTemperature(), question);
		GenerationResult result = gen.call(param);
		// 获取模型响应
		Integer inputTokens = result.getUsage().getInputTokens();
		Integer outputTokens = result.getUsage().getOutputTokens();
		Integer totalTokens = result.getUsage().getTotalTokens();
		String resultMsg = result.getOutput().getChoices().get(0).getMessage().getContent();
		return ChatMessage.builder()
			.role(RoleTypeEnum.ASSISTANT.getRoleName())
			.content(resultMsg)
			.inputTokens(inputTokens)
			.outputTokens(outputTokens)
			.totalTokens(totalTokens)
			.build();
	}

	private static GenerationParam createGenerationParam(List<ChatMessage> memory, String apiKey,
			boolean includeSystemPrompt, String prompt, String modelName, float temperature, String question) {
		List<Message> messages = new ArrayList<>();
		memory.forEach(item -> {
			Message message = Message.builder().role(item.getRole()).content(item.getContent()).build();
			messages.add(message);
		});
		Message userMessage = Message.builder().role(RoleTypeEnum.USER.getRoleName()).content(question).build();
		messages.add(userMessage);
		GenerationParam paramBuilder = GenerationParam.builder()
			.model(modelName)
			.messages(messages)
			.resultFormat(GenerationParam.ResultFormat.MESSAGE)
			.apiKey(apiKey)
			.topK(50)
			.temperature(temperature)
			.topP(0.8)
			.seed(1234)
			.build();

		if (includeSystemPrompt) {
			// 如果为空报错
			String promptStr = Optional.ofNullable(prompt)
				.filter(StringUtils::isNotBlank)
				.orElseThrow(() -> new IllegalArgumentException("Prompt cannot be empty."));
			// 添加系统提示词消息
			Message systemMessage = Message.builder().role(Role.SYSTEM.getValue()).content(promptStr).build();
			// 系统提示词作为对话历史的第一条消息
			messages.add(0, systemMessage);
			paramBuilder.setMessages(messages);
		}
		return paramBuilder;
	}

}

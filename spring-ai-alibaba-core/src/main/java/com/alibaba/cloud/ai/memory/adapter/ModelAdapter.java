package com.alibaba.cloud.ai.memory.adapter;

import com.alibaba.cloud.ai.memory.enums.ModelTypeEnum;
import com.alibaba.cloud.ai.memory.handler.ModelHandler;
import com.alibaba.cloud.ai.memory.entity.ChatMemoryProperties;
import com.alibaba.cloud.ai.memory.entity.ChatMessage;
import com.alibaba.cloud.ai.memory.modelclient.OpenAIModelClient;
import com.alibaba.cloud.ai.memory.modelclient.QwenModelClient;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Title model processing adapter.<br>
 * Description Used to accommodate processing of different model types .<br>
 *
 * @author zhych1005
 * @since 1.0.0-M3
 */

@Component
public class ModelAdapter {

	@Autowired
	private QwenModelClient qwenModelClient;

	@Autowired
	private OpenAIModelClient openAIModelClient;

	private ModelHandler modelClient;

	public void init(String modelName) {
		String manufacturer = ModelTypeEnum.getManufacturer(modelName.toLowerCase());
		switch (manufacturer) {
			case "qwen" -> this.modelClient = qwenModelClient;
			case "openai" -> this.modelClient = openAIModelClient;
			default -> throw new IllegalArgumentException("Unsupported model type: " + modelName);
		}
	}

	public ChatMessage getResponse(List<ChatMessage> messages, String conversationId, String question,
			ChatMemoryProperties properties) throws NoApiKeyException, InputRequiredException {
		return modelClient.getResponse(messages, conversationId, question, properties);
	}

}

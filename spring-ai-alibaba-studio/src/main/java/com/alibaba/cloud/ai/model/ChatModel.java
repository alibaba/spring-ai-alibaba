package com.alibaba.cloud.ai.model;

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatModel {

	@Schema(description = "ChatModel bean name", examples = { "chatModel", "chatModel1" })
	private String name;

	@Schema(description = "dashscope model name",
			examples = { "qwen-plus", "qwen-turbo", "qwen-max", "qwen-max-longcontext" })
	private String model;

	private ModelType modelType;

	private DashScopeChatOptions chatOptions;

	private DashScopeImageOptions imageOptions;

}

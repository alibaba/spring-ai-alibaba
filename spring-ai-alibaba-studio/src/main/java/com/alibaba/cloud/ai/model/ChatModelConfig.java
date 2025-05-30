/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.model;

import com.alibaba.cloud.ai.common.ModelType;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatModelConfig {

	@Schema(description = "ChatModel bean name", examples = { "chatModel", "chatModel1" })
	private String name;

	@Schema(description = "dashscope model name",
			examples = { "qwen-plus", "qwen-turbo", "qwen-max", "qwen-max-longcontext" })
	private String model;

	private ModelType modelType;

	@Schema(nullable = true)
	private DashScopeChatOptions chatOptions;

	@Schema(nullable = true)
	private DashScopeImageOptions imageOptions;

}

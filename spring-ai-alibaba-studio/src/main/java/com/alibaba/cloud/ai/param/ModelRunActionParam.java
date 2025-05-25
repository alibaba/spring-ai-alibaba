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

package com.alibaba.cloud.ai.param;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ModelRunActionParam {

	@Schema(description = "action key, bean name", examples = { "chatModel, chatClient" })
	private String key;

	@Schema(description = "user input")
	private String input;

	@Schema(description = "system prompt")
	private String prompt;

	@Schema(description = "use stream response", defaultValue = "false")
	private Boolean stream = Boolean.FALSE;

	@Schema(description = "chat model config", nullable = true)
	private DashScopeChatOptions chatOptions;

	@Schema(description = "image model config", nullable = true)
	private DashScopeImageOptions imageOptions;

}

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

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.ChatOptions;

@Data
@Builder
public class ChatClient {

	@Schema(description = "ChatClient bean name", examples = { "chatClient", "chatClient1" })
	private String name;

	@Schema(description = "Default System Text",
			examples = { "You are a friendly chat bot that answers question in the voice of a {voice}" })
	private String defaultSystemText;

	@Schema(description = "Default System Params")
	private Map<String, Object> defaultSystemParams;

	@Schema(description = "ChatModel of ChatClient")
	private ChatModelConfig chatModel;

	private ChatOptions chatOptions;

	private List<Advisor> advisors;

	private Boolean isMemoryEnabled;

}

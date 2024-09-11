/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.agent;

import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import reactor.core.publisher.Flux;

/**
 * Title base agent.<br>
 * Description base agent.<br>
 * Created at 2024-08-13 16:58
 *
 * @author yuanci.ytb
 * @version 1.0.0
 * @since jdk8
 */

public abstract class Agent {

	/**
	 * call with chat model
	 * @param prompt user prompt
	 * @return chat response
	 */
	public abstract ChatResponse call(Prompt prompt);

	/**
	 * stream call with chat model
	 * @param prompt user prompt
	 * @return streaming chat response
	 */
	public abstract Flux<ChatResponse> stream(Prompt prompt);

}

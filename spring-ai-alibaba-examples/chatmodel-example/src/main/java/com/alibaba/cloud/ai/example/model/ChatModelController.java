/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.model;

import com.alibaba.cloud.ai.dashscope.api.DashScopeResponseFormat;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class ChatModelController {

	private final ChatModel chatModel;

	public ChatModelController(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	@GetMapping("/chat")
	public String chat(String input) {

		ChatResponse response = chatModel.call(new Prompt(input));
		return response.getResult().getOutput().getContent();
	}

	@GetMapping("/stream")
	public String stream(String input) {

		StringBuilder res = new StringBuilder();
		Flux<ChatResponse> stream = chatModel.stream(new Prompt(input));
		stream.toStream().toList().forEach(resp -> {
			 res.append(resp.getResult().getOutput().getContent());
		});

		return res.toString();
	}

	/**
	 * Tips: When specifying response types as json, you must include json when entering input, otherwise you will receive an error:
	 * 400 - {"code":"InvalidParameter","message":"<400> InternalError.Algo.InvalidParameter: 'messages' must contain the word 'json' in some form, to use 'response_format' of type 'json_object'."
	 *
	 * For example: In this interface, when mode is true, your input should be "Hello, returned in json format", and the prompt must contain the word json
	 * request url: <a href="http://localhost:8080/ai/response_types/true/"你好，以 json 形式返回信息">...</a>
	 *
	 * @return json string
	 */
	@GetMapping("/response_types/{mode}/{input}")
	public String responseTypes(
			@PathVariable(value = "input") String input,
			@PathVariable(value = "mode") Boolean mode
	) {

		DashScopeChatOptions.DashscopeChatOptionsBuilder builder = DashScopeChatOptions.builder();

		if (!mode) {
			builder.withResponseFormat(DashScopeResponseFormat.builder().type(DashScopeResponseFormat.Type.TEXT).build());
		} else {
			builder.withResponseFormat(DashScopeResponseFormat.builder().type(DashScopeResponseFormat.Type.JSON_OBJECT).build());
		}

		return chatModel.call(new Prompt(input, builder.build())).getResult().getOutput().getContent();
	}

}

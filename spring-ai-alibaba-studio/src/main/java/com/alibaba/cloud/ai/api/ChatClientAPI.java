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
package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.ChatClient;
import com.alibaba.cloud.ai.param.ClientRunActionParam;
import com.alibaba.cloud.ai.service.ChatClientDelegate;
import com.alibaba.cloud.ai.vo.ChatClientRunResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "chat-client", description = "the chat-client API")
public interface ChatClientAPI {

	default ChatClientDelegate getDelegate() {
		return new ChatClientDelegate() {
		};
	}

	@Operation(summary = "list chat clients", description = "", tags = { "chat-client" })
	@GetMapping(value = "", produces = { "application/json" })
	default R<List<ChatClient>> list() {
		List<ChatClient> res = getDelegate().list();
		return R.success(res);
	}

	@Operation(summary = "get chat client by name", description = "", tags = { "chat-client" })
	@GetMapping(value = "/{clientName}", produces = { "application/json" })
	default R<ChatClient> get(@PathVariable String clientName) {
		ChatClient chatClient = getDelegate().get(clientName);
		return R.success(chatClient);
	}

	@Operation(summary = "run chat client by input", description = "", tags = { "chat-client" })
	@PostMapping(value = "", consumes = { MediaType.APPLICATION_JSON_VALUE })
	default R<ChatClientRunResult> run(@RequestBody ClientRunActionParam runActionParam) {
		ChatClientRunResult res = getDelegate().run(runActionParam);
		return R.success(res);
	}

}

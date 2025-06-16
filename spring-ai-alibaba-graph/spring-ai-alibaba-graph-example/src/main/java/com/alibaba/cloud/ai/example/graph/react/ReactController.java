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
package com.alibaba.cloud.ai.example.graph.react;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;

import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/react")
public class ReactController {

	private final CompiledGraph compiledGraph;

	ReactController(@Qualifier("reactAgentGraph") CompiledGraph compiledGraph) {
		this.compiledGraph = compiledGraph;
	}

	@GetMapping("/chat")
	public String simpleChat(String query) throws GraphRunnerException {

		Optional<OverAllState> result = compiledGraph.invoke(Map.of("messages", new UserMessage(query)));
		List<Message> messages = (List<Message>) result.get().value("messages").get();
		AssistantMessage assistantMessage = (AssistantMessage) messages.get(messages.size() - 1);

		return assistantMessage.getText();
	}

}

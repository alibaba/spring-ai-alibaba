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
package com.alibaba.cloud.ai.example.graph.reflection;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.GraphStateException;
import com.alibaba.cloud.ai.graph.agent.ReflectAgent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/reflection")
public class ReflectionController {

	private static final Logger logger = LoggerFactory.getLogger(ReflectionController.class);

	private CompiledGraph compiledGraph;

	public ReflectionController(@Qualifier("reflectGraph") CompiledGraph compiledGraph) {
		this.compiledGraph = compiledGraph;
	}

	@GetMapping("/chat")
	public String simpleChat(String query) throws GraphStateException {
		return compiledGraph.invoke(Map.of(ReflectAgent.MESSAGES, List.of(new UserMessage(query))))
			.get()
			.<List<Message>>value(ReflectAgent.MESSAGES)
			.orElseThrow()
			.stream()
			.filter(message -> message.getMessageType() == MessageType.ASSISTANT)
			.reduce((first, second) -> second)
			.map(Message::getText)
			.orElseThrow();
	}

}

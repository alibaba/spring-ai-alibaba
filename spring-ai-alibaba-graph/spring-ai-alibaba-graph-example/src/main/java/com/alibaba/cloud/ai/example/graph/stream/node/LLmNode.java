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
package com.alibaba.cloud.ai.example.graph.stream.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.streaming.StreamingChatGenerator;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import org.apache.commons.collections4.CollectionUtils;
import org.bsc.async.AsyncGenerator;
import org.bsc.async.AsyncGeneratorQueue;
import org.reactivestreams.Subscription;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.CoreSubscriber;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;

@Component
public class LLmNode implements NodeAction {

	private ChatModel chatModel;

	private ChatClient chatClient;

	public LLmNode(ChatModel chatModel,ChatClient chatClient) {
		this.chatModel = chatModel;
	}

	@Override
	public Map<String, Object> apply(OverAllState t) {
		// Create prompt with user message
		UserMessage message = new UserMessage((String) t.value(OverAllState.DEFAULT_INPUT_KEY).get());

		var flux = chatClient.prompt()
				.messages(message)
				.stream()
				.chatResponse();

		var generator  = StreamingChatGenerator.builder()
				.startingNode("llmNode")
				.startingState( t )
				.mapResult( response -> Map.of( "messages", response.getResult().getOutput()))
				.build(flux);

		return Map.of("messages", generator);
	}

}

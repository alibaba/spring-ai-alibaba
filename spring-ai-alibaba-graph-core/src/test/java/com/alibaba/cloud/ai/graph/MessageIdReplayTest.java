/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.checkpoint.config.SaverConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.serializer.StateSerializer;
import com.alibaba.cloud.ai.graph.serializer.plain_text.jackson.SpringAIJacksonStateSerializer;
import com.alibaba.cloud.ai.graph.serializer.std.SpringAIStateSerializer;
import com.alibaba.cloud.ai.graph.state.RemoveByHash;
import com.alibaba.cloud.ai.graph.state.RemoveMessage;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.alibaba.cloud.ai.graph.state.strategy.MessageIdStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import reactor.core.publisher.Flux;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.state.strategy.MessageIdStrategy.MESSAGE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MessageIdReplayTest {

	@TempDir
	Path directory;

	@Test
	void preservesAnonymousMessagesAndExistingHistoryOperations() {
		var reducer = new MessageIdStrategy();
		var first = user("old", "user");
		var last = user("new", "user");
		var anonymous = new UserMessage("repeat");
		assertEquals(List.of(last, anonymous, anonymous),
				reducer.apply(List.of(first, anonymous), List.of(last, anonymous)));
		assertEquals(List.of(), reducer.apply(null, new RemoveMessage("user")));
		assertEquals(List.of(), reducer.apply(List.of(), List.of(first, new RemoveMessage("user"))));
		assertEquals(List.of(anonymous), reducer.apply(List.of(first, anonymous), RemoveByHash.of(first)));
		assertEquals(List.of(last), reducer.apply(List.of(first, anonymous), ReplaceAllWith.of(List.of(last))));
	}

	@Test
	void reenactsOnlyPendingWritesAndKeepsToolCallPairsAcrossJsonAndBinaryRestarts() throws Exception {
		verifyReplay(new SpringAIJacksonStateSerializer(OverAllState::new), Files.createDirectory(directory.resolve("json")));
		verifyReplay(new SpringAIStateSerializer(), Files.createDirectory(directory.resolve("binary")));
	}

	private static void verifyReplay(StateSerializer serializer, Path targetFolder) throws Exception {
		var assistant = AssistantMessage.builder().content("looking up")
				.properties(Map.of(MESSAGE_ID, "assistant", "provider", "test"))
				.toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", "lookup", "{}"))).build();
		var tool = ToolResponseMessage.builder()
				.responses(List.of(new ToolResponseMessage.ToolResponse("call-1", "lookup", "ok")))
				.metadata(Map.of(MESSAGE_ID, "tool")).build();
		var before = user("old", "user");
		var removed = user("discard", "discard");
		var legacyRemoval = new UserMessage("removed by legacy hook");
		var after = user("new", "user");
		var modelCalls = new AtomicInteger();
		var toolCalls = new AtomicInteger();
		var graph = new StateGraph("message-replay",
				() -> Map.of("messages", new MessageIdStrategy(), "labels", new AppendStrategy()), serializer)
				.addNode("model", (state, config) -> {
					modelCalls.incrementAndGet();
					return CompletableFuture.completedFuture(
							Map.of("messages", List.of(after, new RemoveMessage("discard"),
											RemoveByHash.of(legacyRemoval), assistant),
									"labels", ReplaceAllWith.of("new-label")));
				})
				.addNode("tool", (state, config) -> {
					if (toolCalls.incrementAndGet() == 1) {
						return CompletableFuture.completedFuture(Map.of("stream", Flux.error(
								new IllegalStateException("temporary tool failure"))));
					}
					return CompletableFuture.completedFuture(Map.of("messages", List.of(assistant, tool)));
				})
				.addEdge(START, "model").addEdge(START, "tool")
				.addEdge("model", END).addEdge("tool", END);
		var config = RunnableConfig.builder().threadId("tool-pairing").build();
		var input = Map.<String, Object>of("messages", List.of(before, removed, legacyRemoval),
				"labels", List.of("old-label"));
		assertThrows(IllegalStateException.class,
				() -> graph.compile(compileConfig(serializer, targetFolder)).stream(input, config)
						.blockLast(Duration.ofSeconds(5)));

		var restarted = graph.compile(compileConfig(serializer, targetFolder));
		var last = restarted.stream(Map.of(), config.withResume()).blockLast(Duration.ofSeconds(5));
		var messages = last.state().<List<Message>>value("messages").orElseThrow();
		assertEquals(List.of(after, assistant, tool), messages);
		assertEquals(List.of("new-label"), last.state().value("labels").orElseThrow());
		var request = assertInstanceOf(AssistantMessage.class, messages.get(1)).getToolCalls().get(0);
		var response = assertInstanceOf(ToolResponseMessage.class, messages.get(2)).getResponses().get(0);
		assertEquals(request.id(), response.id());
		assertEquals(1, modelCalls.get());
		assertEquals(2, toolCalls.get());
	}

	private static CompileConfig compileConfig(StateSerializer serializer, Path folder) {
		var saver = FileSystemSaver.builder().targetFolder(folder).stateSerializer(serializer).build();
		return CompileConfig.builder().saverConfig(SaverConfig.builder().register(saver).build())
				.taskCheckpoints(true).build();
	}

	private static UserMessage user(String text, String id) {
		return UserMessage.builder().text(text).metadata(Map.of(MESSAGE_ID, id)).build();
	}

}

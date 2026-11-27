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
package com.alibaba.cloud.ai.graph.agent.interceptors;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.PatchToolCallsInterceptor;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.GlobTool;
import com.alibaba.cloud.ai.graph.agent.extension.tools.filesystem.ListFilesTool;
import com.alibaba.cloud.ai.graph.agent.hook.messages.MessagesModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectModelHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;
import com.alibaba.cloud.ai.graph.agent.tool.ToolCallResults;
import com.alibaba.cloud.ai.graph.state.ReplaceAllWith;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.part.MediaPart;
import org.springframework.ai.chat.messages.part.MessagePart;
import org.springframework.ai.chat.messages.part.TextPart;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import static com.alibaba.cloud.ai.graph.agent.hook.returndirect.ReturnDirectConstants.FINISH_REASON_METADATA_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.ai.model.tool.ToolExecutionResult.FINISH_REASON;

class ToolHistoryPartsTest {

	@Test
	void patchesAnEarlierCallEvenWhenALaterTurnReusesItsId() {
		var first = assistant("reused", "search");
		var second = assistant("reused", "search");
		var secondResult = reply("reused", "search");
		var followup = new UserMessage("try again");
		var history = List.<Message>of(first, followup, second, secondResult);

		var patched = patch(history);
		assertEquals(5, patched.size());
		var cancellation = assertInstanceOf(ToolResponseMessage.class, patched.get(1)).getResults().get(0);
		assertEquals("reused", cancellation.id());
		assertTrue(cancellation.isError());
		assertEquals(List.of(followup, second, secondResult), patched.subList(2, 5));
	}

	@Test
	void oneOlderResultWithNoIdOnlySettlesOneCallAndPatchingIsIdempotent() {
		var first = new AssistantMessage.ToolCall(null, "function", "search", "{}");
		var second = new AssistantMessage.ToolCall(null, "function", "search", "{}");
		var assistant = AssistantMessage.builder().toolCalls(List.of(first, second)).build();
		var actual = reply(null, "search");
		var patched = patch(List.of(assistant, actual));
		var cancellation = assertInstanceOf(ToolResponseMessage.class, patched.get(2)).getResults().get(0);
		assertTrue(cancellation.isError());
		assertEquals(ToolCallResults.withCallKey(cancellation, "#1"), cancellation);
		assertSame(actual, patched.get(1));
		assertEquals(patched, patch(patched));
	}

	@Test
	void summarizationDoesNotConnectToolResultsAcrossTurns() {
		var oldTool = assistant("reused", "search");
		var nextUser = new UserMessage("second user turn");
		var recentTool = assistant("reused", "search");
		var recentResult = reply("reused", "search");
		var tail = new AssistantMessage("final");
		var history = List.<Message>of(new UserMessage("old user turn"), oldTool, reply("reused", "search"),
				new AssistantMessage("old final"), nextUser, recentTool, recentResult, tail);
		var hook = SummarizationHook.builder()
				.model(prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("summary")))))
				.messagesToKeep(4).keepFirstUserMessage(false).maxTokensBeforeSummary(0).build();
		var updates = MessagesModelHook.beforeModelAction(hook)
				.apply(new OverAllState(Map.of("messages", history)), RunnableConfig.builder().build()).join();
		var messages = assertInstanceOf(ReplaceAllWith.class, updates.get("messages")).newValues();
		assertEquals(List.of(nextUser, recentTool, recentResult, tail), messages.subList(1, messages.size()));
	}

	@Test
	void returnDirectPreservesMediaAndPublishesTheConfiguredOutputKey() throws Exception {
		var media = MediaPart.of(Media.builder().mimeType(MimeTypeUtils.IMAGE_PNG).data("fixture").build());
		List<MessagePart> parts = List.of(TextPart.of("before"), media, TextPart.of("after"));
		var result = new ToolResultPart("image", "generate", parts, false, null, Map.of());
		var response = ToolResponseMessage.builder().result(result)
				.metadata(Map.of(FINISH_REASON_METADATA_KEY, FINISH_REASON)).build();
		var agent = ReactAgent.builder().name("direct-media").outputKey("answer").hooks(List.of(new ReturnDirectModelHook()))
				.model(prompt -> { throw new AssertionError("returnDirect should skip the model"); }).build();

		var answer = agent.call(List.of(new UserMessage("generate an image"), assistant("image", "generate"), response));
		assertEquals(parts, answer.getParts());
	}

	@Test
	void usesJsonObjectsForBuiltInFilesystemToolArguments(@TempDir Path directory) throws Exception {
		var mapper = new ObjectMapper();
		var glob = GlobTool.createGlobToolCallback("find matching files");
		var ls = ListFilesTool.createListFilesToolCallback("list files");
		var globSchema = mapper.readTree(glob.getToolDefinition().inputSchema());
		var lsSchema = mapper.readTree(ls.getToolDefinition().inputSchema());
		assertEquals("object", globSchema.path("type").asText());
		assertEquals("string", globSchema.path("properties").path("pattern").path("type").asText());
		assertEquals("object", lsSchema.path("type").asText());
		Files.createFile(directory.resolve("fixture.txt"));
		assertTrue(ls.call(mapper.writeValueAsString(Map.of("path", directory.toString()))).contains("fixture.txt"));
	}

	private static AssistantMessage assistant(String id, String name) {
		return AssistantMessage.builder().toolCalls(List.of(new AssistantMessage.ToolCall(id, "function", name, "{}")))
				.build();
	}

	private static ToolResponseMessage reply(String id, String name) {
		return ToolResponseMessage.builder().result(new ToolResultPart(id, name, List.of(TextPart.of("result")),
				false, null, Map.of())).build();
	}

	private static List<Message> patch(List<Message> history) {
		var interceptor = PatchToolCallsInterceptor.builder().build();
		var captured = new AtomicReference<List<Message>>();
		interceptor.interceptModel(ModelRequest.builder().messages(new ArrayList<>(history)).build(), request -> {
			captured.set(request.getMessages());
			return ModelResponse.of(new AssistantMessage("done"));
		});
		return captured.get();
	}

}

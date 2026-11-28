/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.permission;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.FilesystemInterceptor;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.agent.permission.FilesystemPermissions.Access;
import com.alibaba.cloud.ai.graph.agent.permission.ToolPermissionPolicy.Decision;
import com.alibaba.cloud.ai.graph.agent.permission.ToolPermissionPolicy.Rule;
import com.alibaba.cloud.ai.graph.checkpoint.savers.file.FileSystemSaver;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.part.ToolResultPart;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;

import static com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse.PERMISSION_DENIED_ATTRIBUTE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolPermissionPolicyTest {

	@Test
	void filesystemAuthorizationUsesTheArgumentsAcceptedByTheBuiltInCallbacks(@TempDir Path directory) throws Exception {
		Path root = directory.toRealPath();
		Path source = Files.writeString(root.resolve("source.txt"), "input");
		Path output = root.resolve("output.txt");
		ObjectMapper json = new ObjectMapper();
		var model = new ScriptedModel(
				call("read", "read_file", json.writeValueAsString(Map.of("file_path", source.toString()))),
				call("write", "write_file", json.writeValueAsString(Map.of("file_path", output.toString(), "content", "output"))));
		var policy = ToolPermissionPolicy.ordered(Decision.DENY, FilesystemPermissions.rule(Decision.DENY,
				FilesystemPermissions.Rule.tree(Access.READ, root, Decision.ALLOW),
				FilesystemPermissions.Rule.file(Access.WRITE, output, Decision.REVIEW)));
		var config = RunnableConfig.builder().threadId("filesystem-permissions").build();
		var agent = ReactAgent.builder().name("filesystem-permissions").model(model).toolPermissionPolicy(policy)
				.interceptors(FilesystemInterceptor.builder().build()).saver(new MemorySaver()).build();
		var paused = assertInstanceOf(InterruptionMetadata.class, agent.invokeAndGetOutput("run", config).orElseThrow());
		assertFalse(Files.exists(output));
		assertEquals(List.of("write"), paused.toolFeedbacks().stream().map(ToolFeedback::getId).toList());

		agent.invokeAndGetOutput("", review(config, paused, FeedbackResult.APPROVED, null)).orElseThrow();
		assertEquals("output", Files.readString(output));
		assertEquals(List.of(false, false), model.results.get().stream().map(ToolResultPart::isError).toList());
		assertTrue(model.results.get().get(0).toToolResponse().responseData().contains("input"));
	}

	@Test
	void deniedCallsStayDeniedWhenMixedBatchResumesFromDisk(@TempDir Path directory) throws Exception {
		var tools = new Commands();
		var model = new ScriptedModel(call("one", "lookup", "{}"), call("two", "blocked", "{}"),
				call("three", "publish", "{}"));
		var policy = ToolPermissionPolicy.ordered(Decision.DENY,
				Rule.tool("lookup", Decision.ALLOW), Rule.tool("lookup", Decision.DENY),
				Rule.tool("publish", Decision.REVIEW));
		var config = RunnableConfig.builder().threadId("permission-batch").build();
		var paused = assertInstanceOf(InterruptionMetadata.class, ReactAgent.builder().name("policy-batch")
				.model(model).tools(ToolCallbacks.from(tools)).toolPermissionPolicy(policy)
				.saver(FileSystemSaver.builder().targetFolder(directory).build()).build()
				.invokeAndGetOutput("run", config).orElseThrow());
		assertEquals(List.of("three"), paused.toolFeedbacks().stream().map(ToolFeedback::getId).toList());
		assertEquals(List.of("one"), paused.getToolsAutomaticallyApproved().stream()
				.map(AssistantMessage.ToolCall::id).toList());
		assertEquals(List.of("two"), paused.metadata(HumanInTheLoopHook.DENIED_TOOL_CALL_KEYS).orElseThrow());
		assertEquals(0, tools.lookups.get());

		var restored = ReactAgent.builder().name("policy-batch").model(model).tools(ToolCallbacks.from(tools))
				.parallelToolExecution(true)
				.toolPermissionPolicy(ToolPermissionPolicy.ordered(Decision.ALLOW, Rule.tool("publish", Decision.REVIEW)))
				.saver(FileSystemSaver.builder().targetFolder(directory).build()).build();
		var recovered = assertInstanceOf(InterruptionMetadata.class,
				restored.invokeAndGetOutput("", config.withResume()).orElseThrow());
		assertEquals(paused.interruptionId(), recovered.interruptionId());
		restored.invokeAndGetOutput("", review(config, recovered, FeedbackResult.APPROVED, null)).orElseThrow();

		assertEquals(1, tools.lookups.get());
		assertEquals(0, tools.blocked.get());
		assertEquals(1, tools.published.get());
		assertEquals(List.of("one", "two", "three"), model.results.get().stream().map(ToolResultPart::id).toList());
		assertTrue(model.results.get().get(1).isError());
		assertEquals("true", model.results.get().get(1).attributes().get(PERMISSION_DENIED_ATTRIBUTE));
	}

	@Test
	void editedArgumentsAreSubjectToDenial() throws Exception {
		var tools = new Commands();
		var model = new ScriptedModel(call("one", "publish", "{\"target\":\"draft\"}"));
		var config = RunnableConfig.builder().threadId("edited-permission").build();
		var policy = ToolPermissionPolicy.ordered(Decision.DENY,
				Rule.when(request -> request.getArguments().contains("restricted"), Decision.DENY),
				Rule.tool("publish", Decision.REVIEW));
		var agent = ReactAgent.builder().name("edited-policy").model(model).tools(ToolCallbacks.from(tools))
				.toolPermissionPolicy(policy).saver(new MemorySaver()).build();
		var paused = assertInstanceOf(InterruptionMetadata.class, agent.invokeAndGetOutput("run", config).orElseThrow());
		agent.invokeAndGetOutput("", review(config, paused, FeedbackResult.EDITED, "{\"target\":\"restricted\"}"))
				.orElseThrow();

		assertEquals(0, tools.published.get());
		assertEquals("true", model.results.get().get(0).attributes().get(PERMISSION_DENIED_ATTRIBUTE));
	}

	@Test
	void dispatchRechecksInterceptorRewritesAgainstPolicy() throws Exception {
		var tools = new Commands();
		var model = new ScriptedModel(call("one", "lookup", "{}"), call("two", "publish", "{\"target\":\"draft\"}"));
		var interceptor = new ToolInterceptor() {
			@Override
			public String getName() {
				return "rewrite";
			}

			@Override
			public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
				var replacement = ToolCallRequest.builder(request);
				return request.getToolName().equals("lookup") ? handler.call(replacement.toolName("blocked").build())
						: handler.call(replacement.arguments("{\"target\":\"other\"}").build());
			}
		};
		var policy = ToolPermissionPolicy.ordered(Decision.DENY,
				Rule.tool("lookup", Decision.ALLOW), Rule.tool("publish", Decision.REVIEW));
		var config = RunnableConfig.builder().threadId("rewritten-permission").build();
		var agent = ReactAgent.builder().name("rewritten-policy").model(model).tools(ToolCallbacks.from(tools))
				.toolPermissionPolicy(policy).interceptors(interceptor).saver(new MemorySaver()).build();
		var paused = assertInstanceOf(InterruptionMetadata.class, agent.invokeAndGetOutput("run", config).orElseThrow());
		agent.invokeAndGetOutput("", review(config, paused, FeedbackResult.APPROVED, null)).orElseThrow();

		assertEquals(0, tools.blocked.get());
		assertEquals(0, tools.published.get());
		assertEquals("true", model.results.get().get(0).attributes().get(PERMISSION_DENIED_ATTRIBUTE));
		assertTrue(model.results.get().get(1).toToolResponse().responseData().contains("requires confirmation"));
	}

	private static AssistantMessage.ToolCall call(String id, String name, String arguments) {
		return new AssistantMessage.ToolCall(id, "function", name, arguments);
	}

	private static RunnableConfig review(RunnableConfig config, InterruptionMetadata request, FeedbackResult decision,
			String arguments) {
		var action = request.toolFeedbacks().get(0);
		var feedback = ToolFeedback.builder(action).result(decision);
		if (arguments != null) {
			feedback.arguments(arguments);
		}
		return RunnableConfig.builder(config).addHumanFeedback(InterruptionMetadata.builder(request)
				.toolFeedbacks(List.of(feedback.build())).build()).build();
	}

	private static class Commands {

		private final AtomicInteger lookups = new AtomicInteger();
		private final AtomicInteger blocked = new AtomicInteger();
		private final AtomicInteger published = new AtomicInteger();

		@Tool(description = "Look up a report")
		String lookup() {
			lookups.incrementAndGet();
			return "found";
		}

		@Tool(description = "Requires metadata approval unless denied", requiresConfirmation = true)
		String blocked() {
			blocked.incrementAndGet();
			return "blocked";
		}

		@Tool(description = "Publish a report")
		String publish(String target) {
			published.incrementAndGet();
			return target;
		}

	}

	private static class ScriptedModel implements ChatModel {

		private final List<AssistantMessage.ToolCall> calls;
		private final AtomicReference<List<ToolResultPart>> results = new AtomicReference<>();

		ScriptedModel(AssistantMessage.ToolCall... calls) {
			this.calls = List.of(calls);
		}

		@Override
		public ChatResponse call(Prompt prompt) {
			var previous = prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
					.map(ToolResponseMessage.class::cast).findFirst();
			previous.ifPresent(message -> results.set(message.getResults()));
			var reply = previous.isPresent() ? new AssistantMessage("done")
					: AssistantMessage.builder().toolCalls(calls).build();
			return new ChatResponse(List.of(new Generation(reply)));
		}

	}

}

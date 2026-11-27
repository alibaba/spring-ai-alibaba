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

package com.alibaba.cloud.ai.agent.studio.controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.alibaba.cloud.ai.agent.studio.loader.AgentLoader;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.Agent;
import com.alibaba.cloud.ai.graph.agent.AgentTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.hip.HumanInTheLoopHook;
import com.alibaba.cloud.ai.graph.agent.hook.hip.ToolConfig;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExecutionControllerApprovalTest {

	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void versionedEventStreamRedactsToolPayloadAndResumesAcrossRuns() throws Exception {
		var tools = new CountingTools();
		var agent = ReactAgent.builder().name("parent")
			.model(model(prompt -> prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance)
					? reply(new AssistantMessage("resumed answer"))
					: reply(toolCalls(new AssistantMessage.ToolCall("review-call", "function", "review",
							"{\"private\":\"payload-hidden-by-default\"}")))))
			.tools(ToolCallbacks.from(tools)[0])
			.hooks(HumanInTheLoopHook.builder().approvalOn("review", "Review the request").build())
			.saver(new MemorySaver()).build();
		var mvc = mvc(agent);
		var started = sse(mvc, "/run_sse/v2", """
				{"appName":"parent","userId":"user","threadId":"thread",
				 "newMessage":{"messageType":"user","content":"ask"}}
				""");
		assertThat(started).contains("event:run_started", "event:run_paused").doesNotContain("payload-hidden-by-default");
		var pause = eventPayload(started, "run_paused");
		String id = pause.path("data").path("interruptionId").asText();
		assertThat(id).isNotBlank();
		var resumed = sse(mvc, "/resume_sse/v2", approve(id));
		var resumeStart = eventPayload(resumed, "run_started");
		assertThat(resumeStart.path("data").path("resumedInterruptionId").asText()).isEqualTo(id);
		assertThat(resumeStart.path("runId").asText()).isNotEqualTo(pause.path("runId").asText());
		assertThat(resumed).contains("resumed answer", "event:run_completed");
		assertThat(tools.executions.get()).isOne();
	}

	private JsonNode eventPayload(String sse, String name) {
		return sse.lines().filter(line -> line.startsWith("data:")).map(line -> {
			try {
				return mapper.readTree(line.substring(5).trim());
			}
			catch (Exception error) {
				throw new IllegalArgumentException("Invalid SSE data", error);
			}
		}).filter(event -> name.equals(event.path("type").asText())).findFirst().orElseThrow();
	}

	@Test
	void resumesTwoChildApprovalsIndependentlyOverSse() throws Exception {
		var tools = new CountingTools();
		var saver = new MemorySaver();
		var worker = ReactAgent.builder().name("worker").description("Worker that needs approval")
			.model(model(prompt -> prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance)
					? reply(new AssistantMessage("worker complete"))
					: reply(toolCalls(new AssistantMessage.ToolCall("review-call", "function", "review", "{}")))))
			.tools(ToolCallbacks.from(tools)[0])
			.hooks(HumanInTheLoopHook.builder().approvalOn("review", "Review the worker request").build())
			.saver(saver).build();
		var agent = ReactAgent.builder().name("parent")
			.model(model(prompt -> prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance)
					? reply(new AssistantMessage("parent complete"))
					: reply(toolCalls(
							new AssistantMessage.ToolCall("same-call", "function", "worker", "{\"input\":\"left\"}"),
							new AssistantMessage.ToolCall("same-call", "function", "worker", "{\"input\":\"right\"}")))))
			.tools(AgentTool.create(worker)).parallelToolExecution(true).saver(saver).build();
		var mvc = mvc(agent);

		var started = sse(mvc, "/run_sse", """
				{"appName":"parent","userId":"user","threadId":"thread",
				 "newMessage":{"messageType":"user","content":"delegate"}}
				""");
		var combined = approvalMessage(started);
		var pending = combined.path("pendingInterruptions");
		assertThat(pending.size()).isEqualTo(2);
		String leftId = pending.get(0).path("interruptionId").asText();
		String rightId = pending.get(1).path("interruptionId").asText();
		assertThat(leftId).isNotBlank().isEqualTo(combined.path("interruptionId").asText());
		assertThat(rightId).isNotBlank().isNotEqualTo(leftId);
		assertThat(tools.executions.get()).isZero();

		var resumed = sse(mvc, "/resume_sse", approve(rightId));
		assertThat(approvalMessage(resumed).path("interruptionId").asText()).isEqualTo(leftId);
		assertThat(tools.executions.get()).isOne();

		var stale = sse(mvc, "/resume_sse", approve(rightId));
		assertThat(stale).contains("\"error\":true", rightId);
		assertThat(tools.executions.get()).isOne();

		var finished = sse(mvc, "/resume_sse", approve(leftId));
		assertThat(finished).contains("parent complete");
		assertThat(tools.executions.get()).isEqualTo(2);
	}

	@Test
	void exposesAllowedDecisionsAndResumesWithAHumanResponseOverSse() throws Exception {
		var tools = new CountingTools();
		var policy = ToolConfig.builder().allowedDecisions(FeedbackResult.RESPONDED).build();
		var agent = ReactAgent.builder().name("parent")
			.model(model(prompt -> prompt.getInstructions().stream().filter(ToolResponseMessage.class::isInstance)
				.map(ToolResponseMessage.class::cast).findFirst()
				.map(result -> reply(new AssistantMessage(result.getResponses().get(0).responseData())))
				.orElseGet(() -> reply(toolCalls(new AssistantMessage.ToolCall("review-call", "function", "review", "{}"))))))
			.tools(ToolCallbacks.from(tools)[0])
			.hooks(HumanInTheLoopHook.builder().approvalOn("review", policy).build())
			.saver(new MemorySaver()).build();
		var mvc = mvc(agent);

		var started = sse(mvc, "/run_sse", """
				{"appName":"parent","userId":"user","threadId":"thread",
				 "newMessage":{"messageType":"user","content":"ask"}}
				""");
		var approval = approvalMessage(started);
		assertThat(approval.path("toolFeedback").get(0).path("allowedDecisions").toString()).isEqualTo("[\"RESPONDED\"]");
		String id = approval.path("interruptionId").asText();
		assertThat(sse(mvc, "/resume_sse", approve(id))).contains("APPROVED is not allowed");
		assertThat(sse(mvc, "/resume_sse", """
				{"appName":"parent","userId":"user","threadId":"thread","interruptionId":"%s",
				 "toolFeedbacks":[{"id":"review-call","name":"review"}]}
				""".formatted(id))).contains("Decision null is not allowed");
		assertThat(sse(mvc, "/resume_sse", """
				{"appName":"parent","userId":"user","threadId":"thread","interruptionId":"%s",
				 "toolFeedbacks":[{"id":"review-call","name":"review","result":"RESPONDED","response":"Option B"}]}
				""".formatted(id))).contains("Option B");
		assertThat(tools.executions.get()).isZero();
	}

	private static MockMvc mvc(Agent agent) {
		return MockMvcBuilders.standaloneSetup(new ExecutionController(new AgentLoader() {

			@Override
			public List<String> listAgents() {
				return List.of(agent.name());
			}

			@Override
			public Agent loadAgent(String name) {
				return agent;
			}

		})).build();
	}

	private String sse(MockMvc mvc, String path, String body) throws Exception {
		var response = mvc.perform(post(path).contentType(MediaType.APPLICATION_JSON)
			.accept(MediaType.TEXT_EVENT_STREAM).content(body)).andExpect(request().asyncStarted()).andReturn();
		return mvc.perform(asyncDispatch(response)).andExpect(status().isOk()).andReturn().getResponse()
			.getContentAsString();
	}

	private JsonNode approvalMessage(String sse) throws Exception {
		for (String line : sse.split("\\R")) {
			if (line.startsWith("data:")) {
				JsonNode message = mapper.readTree(line.substring("data:".length())).path("message");
				if ("tool-confirm".equals(message.path("messageType").asText())) {
					return message;
				}
			}
		}
		throw new AssertionError("Missing tool approval message in " + sse);
	}

	private static String approve(String interruptionId) {
		return """
				{"appName":"parent","userId":"user","threadId":"thread","interruptionId":"%s",
				 "toolFeedbacks":[{"id":"review-call","name":"review","arguments":"{}","result":"APPROVED"}]}
				""".formatted(interruptionId);
	}

	private static ChatResponse reply(AssistantMessage message) {
		return new ChatResponse(List.of(new Generation(message)));
	}

	private static ChatModel model(Function<Prompt, ChatResponse> response) {
		return new ChatModel() {

			@Override
			public ChatResponse call(Prompt prompt) {
				return response.apply(prompt);
			}

			@Override
			public Flux<ChatResponse> stream(Prompt prompt) {
				return Flux.defer(() -> Flux.just(call(prompt)));
			}

		};
	}

	private static AssistantMessage toolCalls(AssistantMessage.ToolCall... toolCalls) {
		return AssistantMessage.builder().toolCalls(List.of(toolCalls)).build();
	}

	static class CountingTools {

		private final AtomicInteger executions = new AtomicInteger();

		@Tool(name = "review", description = "Run the approved worker action")
		public String review() {
			executions.incrementAndGet();
			return "approved";
		}

	}

}

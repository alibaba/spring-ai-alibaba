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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.extension.interceptor.FilesystemInterceptor;
import com.alibaba.cloud.ai.graph.agent.permission.FilesystemPermissions;
import com.alibaba.cloud.ai.graph.agent.permission.FilesystemPermissions.Access;
import com.alibaba.cloud.ai.graph.agent.permission.FilesystemPermissions.Rule;
import com.alibaba.cloud.ai.graph.agent.permission.ToolPermissionPolicy;
import com.alibaba.cloud.ai.graph.agent.permission.ToolPermissionPolicy.Decision;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.util.FileSystemUtils;

/** Runs a local filesystem permission and review flow without a model API key. */
public class ToolPermissionsExample {

	private static final Logger logger = LoggerFactory.getLogger(ToolPermissionsExample.class);

	private static final ObjectMapper JSON = new ObjectMapper();

	public static void main(String[] args) throws Exception {
		Path workspace = Files.createTempDirectory("tool-permissions-").toRealPath();
		try {
			run(workspace);
		}
		finally {
			FileSystemUtils.deleteRecursively(workspace);
		}
	}

	private static void run(Path workspace) throws Exception {
		Path readme = Files.writeString(workspace.resolve("readme.txt"), "Public project information");
		Path restricted = Files.writeString(workspace.resolve("restricted.txt"), "Internal project information");
		Path reports = workspace.resolve("reports");
		Path report = reports.resolve("audit.txt");

		var policy = ToolPermissionPolicy.ordered(Decision.DENY,
				FilesystemPermissions.rule(Decision.DENY,
						Rule.file(Access.READ, restricted, Decision.DENY),
						Rule.tree(Access.READ, workspace, Decision.ALLOW),
						Rule.tree(Access.WRITE, reports, Decision.REVIEW)));

		var calls = List.of(
				call("read", "read_file", Map.of("file_path", readme.toString())),
				call("restricted", "read_file", Map.of("file_path", restricted.toString())),
				call("write", "write_file", Map.of("file_path", report.toString(), "content", "Audit complete.")));
		ChatModel model = prompt -> {
			var reply = prompt.getInstructions().stream().anyMatch(ToolResponseMessage.class::isInstance)
					? new AssistantMessage("Finished reviewing the workspace.")
					: AssistantMessage.builder().toolCalls(calls).build();
			return new ChatResponse(List.of(new Generation(reply)));
		};

		var agent = ReactAgent.builder().name("report-writer").model(model)
				.interceptors(FilesystemInterceptor.builder().build())
				.toolPermissionPolicy(policy).saver(new MemorySaver()).build();
		var config = RunnableConfig.builder().threadId("workspace-audit").build();
		var paused = (InterruptionMetadata) agent.invokeAndGetOutput("Review the workspace", config).orElseThrow();
		logger.info("Review requested for {}. Report exists before approval: {}",
				paused.toolFeedbacks().stream().map(ToolFeedback::getName).toList(), Files.exists(report));

		var approved = ToolFeedback.builder(paused.toolFeedbacks().get(0)).result(FeedbackResult.APPROVED).build();
		var feedback = InterruptionMetadata.builder(paused).toolFeedbacks(List.of(approved)).build();
		var resumed = RunnableConfig.builder(config).addHumanFeedback(feedback).build();

		var output = agent.invokeAndGetOutput("", resumed).orElseThrow();
		for (Message message : output.state().<List<Message>>value("messages").orElseThrow()) {
			if (message instanceof ToolResponseMessage tools) {
				tools.getResults().forEach(result -> logger.info("{} error={} result={}",
						result.id(), result.isError(), result.toToolResponse().responseData()));
			}
		}
		logger.info("Report after approval: {}", Files.readString(report));
	}

	private static AssistantMessage.ToolCall call(String id, String name, Map<String, String> arguments)
			throws JsonProcessingException {
		return new AssistantMessage.ToolCall(id, "function", name, JSON.writeValueAsString(arguments));
	}

}

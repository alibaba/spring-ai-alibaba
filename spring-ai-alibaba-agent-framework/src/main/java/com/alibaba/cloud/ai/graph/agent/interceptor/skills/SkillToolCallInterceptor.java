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
package com.alibaba.cloud.ai.graph.agent.interceptor.skills;

import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolCallResponse;
import com.alibaba.cloud.ai.graph.agent.interceptor.ToolInterceptor;
import com.alibaba.cloud.ai.graph.skills.registry.SkillRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.util.json.JsonParser;

import java.util.Map;
import java.util.Objects;

/**
 * Recovers when a model emits a registered skill name as a tool name.
 *
 * <p>Skill names are identifiers for {@code read_skill}, not standalone tools. Some models
 * nevertheless emit the identifier directly, especially when other tools are available. The
 * regular tool handler is invoked first so a real tool with the same name always wins.
 */
public class SkillToolCallInterceptor extends ToolInterceptor {

	private static final Logger logger = LoggerFactory.getLogger(SkillToolCallInterceptor.class);

	private final SkillRegistry skillRegistry;

	private final ToolCallback readSkillTool;

	public SkillToolCallInterceptor(SkillRegistry skillRegistry, ToolCallback readSkillTool) {
		this.skillRegistry = Objects.requireNonNull(skillRegistry, "skillRegistry must not be null");
		this.readSkillTool = Objects.requireNonNull(readSkillTool, "readSkillTool must not be null");
	}

	@Override
	public ToolCallResponse interceptToolCall(ToolCallRequest request, ToolCallHandler handler) {
		ToolCallResponse response = handler.call(request);
		if (!isUnresolvedToolResponse(response, request) || !skillRegistry.contains(request.getToolName())) {
			return response;
		}

		try {
			String arguments = JsonParser.toJson(Map.of("skill_name", request.getToolName()));
			String content = readSkillTool.call(arguments, new ToolContext(request.getContext()));
			return ToolCallResponse.success(request.getToolCallId(), request.getToolName(), content);
		}
		catch (Exception e) {
			logger.warn("Failed to recover skill tool call '{}' through read_skill", request.getToolName(), e);
			return ToolCallResponse.error(request.getToolCallId(), request.getToolName(), e);
		}
	}

	private boolean isUnresolvedToolResponse(ToolCallResponse response, ToolCallRequest request) {
		return response != null
				&& response.isError()
				&& request != null
				&& request.getToolName() != null
				&& request.getToolName().equals(response.getMetadata().get("unresolvedToolName"));
	}

	@Override
	public String getName() {
		return "SkillToolCallFallback";
	}

}

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
package com.alibaba.cloud.ai.agent.agui;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

public interface AGUIType {

	/**
	 * Represents a function call with a name and arguments.
	 * Corresponds to Zod: {@code FunctionCallSchema}
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record FunctionCall(
			@JsonProperty("name") String name,
			@JsonProperty("arguments") String arguments
	) { }

	/**
	 * Represents a tool call, typically a function call.
	 * Corresponds to Zod: {@code ToolCallSchema}
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record ToolCall(
			@JsonProperty("id") String id,
			@JsonProperty("type") String type, // Expected to be "function"
			@JsonProperty("function") FunctionCall function
	) {
		public ToolCall {
			Objects.requireNonNull(id, "id cannot be null");
			Objects.requireNonNull(type, "type cannot be null");
			Objects.requireNonNull(function, "function cannot be null");
		}
	}

	/**
	 * Represents contextual information.
	 * Corresponds to Zod: {@code ContextSchema}
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Context(
			@JsonProperty("description") String description,
			@JsonProperty("value") String value
	) {
		public Context {
			Objects.requireNonNull(description, "description cannot be null");
			Objects.requireNonNull(value, "value cannot be null");
		}
	}

	/**
	 * Represents a tool definition.
	 * Corresponds to Zod: {@code ToolSchema}
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record Tool(
			@JsonProperty("name") String name,
			@JsonProperty("description") String description,
			@JsonProperty("parameters") Object parameters // Represents JSON Schema
	) {
		public Tool {
			Objects.requireNonNull(name, "name cannot be null");
			Objects.requireNonNull(description, "description cannot be null");
			Objects.requireNonNull(parameters, "parameters cannot be null");
		}
	}

	/**
	 * Input for running an agent.
	 * Corresponds to Zod: {@code RunAgentInputSchema}
	 */
	@JsonIgnoreProperties(ignoreUnknown = true) // Add this annotation
	@JsonInclude(JsonInclude.Include.NON_NULL)
	record RunAgentInput(
			@JsonProperty("threadId") String threadId,
			@JsonProperty("userId") String userId,
			@JsonProperty("runId") String runId,
			@JsonProperty("state") Object state, // Corresponds to z.any()
			@JsonProperty("messages") List<AGUIMessage> messages,
			@JsonProperty("tools") List<Tool> tools,
			@JsonProperty("context") List<Context> context,
			@JsonProperty("forwardedProps") Object forwardedProps,
			@JsonProperty("appName") String appName
	) {
		public RunAgentInput {
			Objects.requireNonNull(threadId, "threadId cannot be null");
			//Objects.requireNonNull(runId, "runId cannot be null");
			// state can be null/any
			Objects.requireNonNull(messages, "messages cannot be null");
//            Objects.requireNonNull(appName, "appName cannot be null");
			//Objects.requireNonNull(tools, "tools cannot be null");
			//Objects.requireNonNull(context, "context cannot be null");
			// forwardedProps can be null/any
		}

		public Optional<AGUIMessage.TextMessage> lastUserMessage() {
			return messages().stream()
					.filter(m -> m instanceof AGUIMessage.TextMessage)
					.map(AGUIMessage.TextMessage.class::cast)
					.filter(AGUIMessage.HasRole::isUser)
					.reduce((first, second) -> second);

		}

		public Optional<AGUIMessage.ResultMessage> lastResultMessage() {
			return messages().stream()
					.filter(m -> m instanceof AGUIMessage.ResultMessage)
					.map(AGUIMessage.ResultMessage.class::cast)
					.reduce((first, second) -> second);
		}
	}

	// StateSchema is z.any(), so it's directly represented by Object.
	// No specific 'State' record is strictly needed unless for nominal typing,
	// in which case it would be: public record State(@JsonProperty("value") Object value) {}

	/**
	 * Custom error class for AG-UI related errors.
	 * Corresponds to TypeScript: {@code AGUIError}
	 */
	class AGUIError extends RuntimeException {
		public AGUIError(String message) {
			super(message);
		}

		public AGUIError(String message, Throwable cause) {
			super(message, cause);
		}
	}
}

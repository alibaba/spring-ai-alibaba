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

package com.alibaba.cloud.ai.observation.model.semconv;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public final class InputOutputModel {

	public interface MessagePart {

	}

	@JsonClassDescription("Chat message")
	public record ChatMessage(
			@JsonProperty(required = true, value = "role") @JsonPropertyDescription("Role of response") String role,
			@JsonProperty(required = true,
					value = "parts") @JsonPropertyDescription("List of message parts that make up the message content") List<MessagePart> parts) {
	}

	@JsonClassDescription("Output message")
	public record OutputMessage(
			@JsonProperty(required = true, value = "role") @JsonPropertyDescription("Role of response") String role,
			@JsonProperty(required = true,
					value = "parts") @JsonPropertyDescription("List of message parts that make up the message content") List<MessagePart> parts,
			@JsonProperty(required = true,
					value = "finish_reason") @JsonPropertyDescription("Reason for finishing the generation") String finishReason) {
	}

	public enum RoleEnum {

		UNKNOWN("unknown"), USER("user"), ASSISTANT("assistant"), SYSTEM("system"), TOOL("tool");

		public final String value;

		RoleEnum(String value) {
			this.value = value;
		}

	}

	@JsonClassDescription("Text content sent to or received from the model")
	public record TextPart(@JsonProperty(required = true,
			value = "type") @JsonPropertyDescription("The type of the content captured in this part") String type,
			@JsonProperty(required = true,
					value = "content") @JsonPropertyDescription("Text content sent to or received from the model") String content)
			implements
				MessagePart {
	}

	@JsonClassDescription("A tool call requested by the model")
	public record ToolCallRequestPart(@JsonProperty(required = true,
			value = "type") @JsonPropertyDescription("The type of the content captured in this part") String type,
			@JsonProperty(required = true, value = "name") @JsonPropertyDescription("Name of the tool") String name,
			@JsonProperty(value = "id") @JsonPropertyDescription("Unique identifier for the tool call") String id,
			@JsonProperty(value = "arguments") @JsonPropertyDescription("Arguments for the tool call") String arguments)
			implements
				MessagePart {
	}

	@JsonClassDescription("A tool call result sent to the model or a built-in tool call outcome and details")
	public record ToolCallResponsePart(@JsonProperty(required = true,
			value = "type") @JsonPropertyDescription("The type of the content captured in this part") String type,
			@JsonProperty(required = true,
					value = "response") @JsonPropertyDescription("Tool call response") String response,
			@JsonProperty(value = "id") @JsonPropertyDescription("Unique tool call identifier") String id)
			implements
				MessagePart {
	}

}

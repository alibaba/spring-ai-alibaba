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
package com.alibaba.cloud.ai.graph.agent.tool;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ToolOutputEnvelope {

	public static final String CONTEXT_KEY = "_TOOL_OUTPUT_ENVELOPE_";
	public static final String METADATA_KEY = "_TOOL_OUTPUT_ENVELOPE_";

	public static final String STATUS_OK = "ok";
	public static final String STATUS_PARTIAL = "partial";
	public static final String STATUS_ERROR = "error";
	public static final String STATUS_INTERRUPTED = "interrupted";

	private final String status;
	private final String toolCallId;
	private final String toolName;
	private final Object payload;
	private final Map<String, Object> artifacts;
	private final Map<String, Object> error;
	private final String resumeHint;
	private final boolean requiresApproval;
	private final String approvalDescription;

	private ToolOutputEnvelope(Builder builder) {
		this.status = builder.status;
		this.toolCallId = builder.toolCallId;
		this.toolName = builder.toolName;
		this.payload = builder.payload;
		this.artifacts = builder.artifacts != null ? new HashMap<>(builder.artifacts) : null;
		this.error = builder.error != null ? new HashMap<>(builder.error) : null;
		this.resumeHint = builder.resumeHint;
		this.requiresApproval = builder.requiresApproval;
		this.approvalDescription = builder.approvalDescription;
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getStatus() {
		return status;
	}

	public String getToolCallId() {
		return toolCallId;
	}

	public String getToolName() {
		return toolName;
	}

	public Object getPayload() {
		return payload;
	}

	public Map<String, Object> getArtifacts() {
		return artifacts != null ? new HashMap<>(artifacts) : null;
	}

	public Map<String, Object> getError() {
		return error != null ? new HashMap<>(error) : null;
	}

	public String getResumeHint() {
		return resumeHint;
	}

	public boolean isRequiresApproval() {
		return requiresApproval;
	}

	public String getApprovalDescription() {
		return approvalDescription;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new HashMap<>();
		map.put("status", status);
		map.put("toolCallId", toolCallId);
		map.put("toolName", toolName);
		if (payload != null) {
			map.put("payload", payload);
		}
		if (artifacts != null && !artifacts.isEmpty()) {
			map.put("artifacts", artifacts);
		}
		if (error != null && !error.isEmpty()) {
			map.put("error", error);
		}
		if (resumeHint != null) {
			map.put("resumeHint", resumeHint);
		}
		if (requiresApproval) {
			map.put("requiresApproval", true);
		}
		if (approvalDescription != null) {
			map.put("approvalDescription", approvalDescription);
		}
		return map;
	}

	public static ToolOutputEnvelope fromMap(Map<String, Object> map) {
		if (map == null || map.isEmpty()) {
			return null;
		}
		Builder builder = builder();
		Object status = map.get("status");
		Object toolCallId = map.get("toolCallId");
		Object toolName = map.get("toolName");
		builder.status(status != null ? status.toString() : null);
		builder.toolCallId(toolCallId != null ? toolCallId.toString() : null);
		builder.toolName(toolName != null ? toolName.toString() : null);
		builder.payload(map.get("payload"));
		Object artifacts = map.get("artifacts");
		if (artifacts instanceof Map<?, ?> artifactsMap) {
			builder.artifacts(castMap(artifactsMap));
		}
		Object error = map.get("error");
		if (error instanceof Map<?, ?> errorMap) {
			builder.error(castMap(errorMap));
		}
		Object resumeHint = map.get("resumeHint");
		if (resumeHint != null) {
			builder.resumeHint(resumeHint.toString());
		}
		Object requiresApproval = map.get("requiresApproval");
		if (requiresApproval instanceof Boolean bool) {
			builder.requiresApproval(bool);
		}
		Object approvalDescription = map.get("approvalDescription");
		if (approvalDescription != null) {
			builder.approvalDescription(approvalDescription.toString());
		}
		return builder.build();
	}

	private static Map<String, Object> castMap(Map<?, ?> source) {
		Map<String, Object> result = new HashMap<>();
		for (Map.Entry<?, ?> entry : source.entrySet()) {
			if (entry.getKey() != null) {
				result.put(entry.getKey().toString(), entry.getValue());
			}
		}
		return result;
	}

	public static class Builder {
		private String status;
		private String toolCallId;
		private String toolName;
		private Object payload;
		private Map<String, Object> artifacts;
		private Map<String, Object> error;
		private String resumeHint;
		private boolean requiresApproval;
		private String approvalDescription;

		public Builder status(String status) {
			this.status = status;
			return this;
		}

		public Builder toolCallId(String toolCallId) {
			this.toolCallId = toolCallId;
			return this;
		}

		public Builder toolName(String toolName) {
			this.toolName = toolName;
			return this;
		}

		public Builder payload(Object payload) {
			this.payload = payload;
			return this;
		}

		public Builder artifacts(Map<String, Object> artifacts) {
			this.artifacts = artifacts;
			return this;
		}

		public Builder error(Map<String, Object> error) {
			this.error = error;
			return this;
		}

		public Builder resumeHint(String resumeHint) {
			this.resumeHint = resumeHint;
			return this;
		}

		public Builder requiresApproval(boolean requiresApproval) {
			this.requiresApproval = requiresApproval;
			return this;
		}

		public Builder approvalDescription(String approvalDescription) {
			this.approvalDescription = approvalDescription;
			return this;
		}

		public ToolOutputEnvelope build() {
			Objects.requireNonNull(status, "status must not be null");
			return new ToolOutputEnvelope(this);
		}
	}
}

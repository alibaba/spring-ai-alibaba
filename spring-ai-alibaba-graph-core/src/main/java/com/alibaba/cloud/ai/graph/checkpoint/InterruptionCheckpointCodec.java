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
package com.alibaba.cloud.ai.graph.checkpoint;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata.ToolFeedback.FeedbackResult;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alibaba.cloud.ai.graph.checkpoint.CheckpointWrite.decode;
import static com.alibaba.cloud.ai.graph.checkpoint.CheckpointWrite.encode;

/** Stores interruption trees using portable checkpoint values. */
public final class InterruptionCheckpointCodec {

	private InterruptionCheckpointCodec() {
	}

	public static Map<String, Object> encodePause(InterruptionMetadata interruption) {
		var data = new HashMap<>(encodeRequest(interruption));
		data.put("state", encode(interruption.state().data()));
		return data;
	}

	/** Encodes a request whose state is already stored in its containing checkpoint. */
	public static Map<String, Object> encodeRequest(InterruptionMetadata interruption) {
		var decisions = interruption.toolFeedbacks().stream().map(feedback -> {
			Map<String, Object> decision = new HashMap<>();
			decision.put("id", feedback.getId());
			decision.put("name", feedback.getName());
			decision.put("arguments", feedback.getArguments());
			decision.put("result", feedback.getResult() == null ? null : feedback.getResult().name());
			decision.put("description", feedback.getDescription());
			decision.put("allowedDecisions", feedback.getAllowedDecisions().stream().map(FeedbackResult::name).toList());
			decision.put("response", feedback.getResponse());
			return decision;
		}).toList();
		var approved = interruption.getToolsAutomaticallyApproved().stream().map(call -> {
			Map<String, Object> tool = new HashMap<>();
			tool.put("id", call.id());
			tool.put("name", call.name());
			tool.put("arguments", call.arguments());
			tool.put("type", call.type());
			return tool;
		}).toList();
		return Map.of("node", interruption.node(),
				"metadata", encode(interruption.metadata().orElseGet(Map::of)), "decisions", decisions, "approved",
				approved, "children",
				interruption.pendingInterruptions().stream().map(InterruptionCheckpointCodec::encodePause).toList());
	}

	@SuppressWarnings("unchecked")
	public static InterruptionMetadata decodePause(Map<String, Object> data) {
		return decodeRequest(data, new OverAllState(decode((Map<String, Object>) data.get("state"))));
	}

	@SuppressWarnings("unchecked")
	public static InterruptionMetadata decodeRequest(Map<String, Object> data, OverAllState state) {
		var decisions = ((List<Map<String, Object>>) data.get("decisions")).stream()
				.map(decision -> {
					var builder = ToolFeedback.builder().id((String) decision.get("id"))
							.name((String) decision.get("name")).arguments((String) decision.get("arguments"))
							.description((String) decision.get("description")).response((String) decision.get("response"));
					if (decision.get("result") instanceof String result) {
						builder.type(result);
					}
					if (decision.get("allowedDecisions") instanceof List<?> allowed) {
						builder.allowedDecisions(allowed.stream().map(Object::toString).map(FeedbackResult::valueOf).toList());
					}
					return builder.build();
				})
				.toList();
		var approved = ((List<Map<String, Object>>) data.get("approved")).stream()
				.map(call -> new ToolCall((String) call.get("id"), (String) call.get("type"),
						(String) call.get("name"), (String) call.get("arguments")))
				.toList();
		var children = ((List<Map<String, Object>>) data.get("children")).stream()
				.map(InterruptionCheckpointCodec::decodePause)
				.toList();
		return new InterruptionMetadata.Builder(decode((Map<String, Object>) data.get("metadata")))
				.nodeId((String) data.get("node")).state(state)
				.toolFeedbacks(decisions).toolsAutomaticallyApproved(approved).pendingInterruptions(children).build();
	}

}

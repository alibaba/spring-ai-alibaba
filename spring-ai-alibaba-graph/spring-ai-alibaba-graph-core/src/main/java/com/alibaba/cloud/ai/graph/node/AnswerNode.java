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

package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Render the template in AnswerNodeData into the final answer string.
 */
public class AnswerNode implements NodeAction {

	private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{\\s*(.+?)\\s*\\}\\}");

	private final String answerTemplate;

	private AnswerNode(String answerTemplate) {
		this.answerTemplate = answerTemplate;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) {
		// Replace {{key}} in the answerTemplate with the value of state.get(key).
		StringBuffer sb = new StringBuffer();
		Matcher matcher = PLACEHOLDER_PATTERN.matcher(answerTemplate);
		while (matcher.find()) {
			String key = matcher.group(1);
			Object val = state.value(key).orElse("");
			String replacement = val != null ? val.toString() : "";
			replacement = replacement.replace("\\", "\\\\").replace("$", "\\$");
			matcher.appendReplacement(sb, replacement);
		}
		matcher.appendTail(sb);
		String resolved = sb.toString();

		// Write the final result back to the state with the key name fixed to "answer"
		Map<String, Object> result = new HashMap<>();
		result.put("answer", resolved);
		return result;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String answerTemplate;

		public Builder answer(String answerTemplate) {
			this.answerTemplate = answerTemplate;
			return this;
		}

		public AnswerNode build() {
			return new AnswerNode(answerTemplate);
		}

	}

}

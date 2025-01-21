/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PromptNode implements NodeAction {

	private Function<String, Map<String, String>> inputMapFunc;

	public PromptNode(String template) {
		this.template = template;
	}

	public PromptNode(String template, Function<String, Map<String, String>> function) {
		this.template = template;
		this.inputMapFunc = function;
	}

	// 可以通过前端映射
	private final String template;

	@Override
	public Map<String, Object> apply(NodeState agentState) {
		Pattern pattern = Pattern.compile("#\\{(.*?)\\}");
		Matcher matcher = pattern.matcher(template);
		StringBuilder sb = new StringBuilder();
		boolean anyFind = false;
		while (matcher.find()) {
			anyFind = true;
			String key = matcher.group(1);
			if (inputMapFunc != null) {
				var input = agentState.input()
					.filter(StringUtils::hasText)
					.orElseThrow(() -> new IllegalArgumentException("no input provided!"));
				Map<String, String> mapV = inputMapFunc.apply(input);
				if (mapV.containsKey(key)) {
					String replacement = mapV.get(key);
					matcher.appendReplacement(sb, replacement != null ? replacement : "");
				}

			}
			else if (agentState.data().containsKey(key)) {
				String replacement = agentState.data().get(key).toString();
				matcher.appendReplacement(sb, replacement != null ? replacement : "");
			}
		}
		matcher.appendTail(sb);
		String content = anyFind ? sb.toString() : template;

		return Map.of(NodeState.OUTPUT, content);
	}

}

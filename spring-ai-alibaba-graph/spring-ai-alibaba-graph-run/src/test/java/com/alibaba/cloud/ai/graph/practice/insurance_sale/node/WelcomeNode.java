package com.alibaba.cloud.ai.graph.practice.insurance_sale.node;

import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.cloud.ai.graph.state.NodeState;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WelcomeNode implements NodeAction<NodeState> {

	public WelcomeNode(String template) {
		this.template = template;
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
			if (agentState.data().containsKey(key)) {
				String replacement = agentState.data().get(key).toString();
				matcher.appendReplacement(sb, replacement != null ? replacement : "");
			}
		}
		matcher.appendTail(sb);
		String content = anyFind ? sb.toString() : template;
		return Map.of(NodeState.OUTPUT, content);
	}

}

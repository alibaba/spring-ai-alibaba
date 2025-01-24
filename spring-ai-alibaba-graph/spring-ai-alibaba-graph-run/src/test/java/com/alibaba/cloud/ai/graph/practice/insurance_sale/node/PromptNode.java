//package com.alibaba.cloud.ai.graph.practice.insurance_sale.node;
//
//import com.alibaba.cloud.ai.graph.OverAllState;
//import com.alibaba.cloud.ai.graph.action.NodeAction;
//import com.alibaba.cloud.ai.graph.state.NodeState;
//import org.springframework.util.StringUtils;
//
//import java.util.Map;
//import java.util.function.Function;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class PromptNode implements NodeAction {
//
//	private Function<String, Map<String, String>> inputMapFunc;
//
//	public PromptNode(String template) {
//		this.template = template;
//	}
//
//	public PromptNode(String template, Function<String, Map<String, String>> function) {
//		this.template = template;
//		this.inputMapFunc = function;
//	}
//
//	// 可以通过前端映射
//	private final String template;
//
//	@Override
//	public Map<String, Object> apply(OverAllState agentState) {
//		Pattern pattern = Pattern.compile("#\\{(.*?)\\}");
//		Matcher matcher = pattern.matcher(template);
//		StringBuilder sb = new StringBuilder();
//		boolean anyFind = false;
//		while (matcher.find()) {
//			anyFind = true;
//			String key = matcher.group(1);
//			if (inputMapFunc != null) {
//				var input = agentState.input()
//					.filter(StringUtils::hasText)
//					.orElseThrow(() -> new IllegalArgumentException("no input provided!"));
//				Map<String, String> mapV = inputMapFunc.apply(input);
//				if (mapV.containsKey(key)) {
//					String replacement = mapV.get(key);
//					matcher.appendReplacement(sb, replacement != null ? replacement : "");
//				}
//
//			}
//			else if (agentState.data().containsKey(key)) {
//				String replacement = agentState.data().get(key).toString();
//				matcher.appendReplacement(sb, replacement != null ? replacement : "");
//			}
//		}
//		matcher.appendTail(sb);
//		String content = anyFind ? sb.toString() : template;
//
//		return Map.of(NodeState.OUTPUT, content);
//	}
//
//}

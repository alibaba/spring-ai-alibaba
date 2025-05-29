package com.alibaba.cloud.ai.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Render the template in AnswerNodeData into the final answer string.
 * <p>
 * Support using {{varName}} placeholders in templates to read and replace the
 * corresponding values from the global state.
 * </p>
 */
public class AnswerNode implements NodeAction {

	public static final String OUTPUT_KEY = "answer";

	private final String answerTemplate;

	private AnswerNode(String answerTemplate) {
		this.answerTemplate = answerTemplate;
	}

	@Override
	public Map<String, Object> apply(OverAllState state) {
		Pattern p = Pattern.compile("\\{\\{(.+?)}}");
		Matcher m = p.matcher(this.answerTemplate);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			String var = m.group(1).trim();
			String val = Optional.of(state.value(var).orElse("")).map(Object::toString).orElse("");
			m.appendReplacement(sb, Matcher.quoteReplacement(val));
		}
		m.appendTail(sb);

		Map<String, Object> out = new HashMap<>();
		out.put(OUTPUT_KEY, sb.toString());
		return out;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String answerTemplate;

		public Builder answer(String tpl) {
			this.answerTemplate = tpl;
			return this;
		}

		public AnswerNode build() {
			return new AnswerNode(this.answerTemplate);
		}

	}

}

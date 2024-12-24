package com.alibaba.cloud.ai.service.dsl.nodes;

import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.AnswerNodeData;
import com.alibaba.cloud.ai.service.dsl.AbstractNodeDataConverter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.utils.StringTemplateUtil;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Stream;

@Component
public class AnswerNodeDataConverter extends AbstractNodeDataConverter<AnswerNodeData> {

	@Override
	public Boolean supportNodeType(NodeType nodeType) {
		return NodeType.ANSWER.equals(nodeType);
	}

	@Override
	protected List<DialectConverter<AnswerNodeData>> getDialectConverters() {
		return Stream.of(AnswerNodeDialectConverter.values())
			.map(AnswerNodeDialectConverter::dialectConverter)
			.toList();
	}

	private enum AnswerNodeDialectConverter {

		DIFY(new DialectConverter<>() {
			@Override
			public Boolean supportDialect(DSLDialectType dialectType) {
				return DSLDialectType.DIFY.equals(dialectType);
			}

			@Override
			public AnswerNodeData parse(Map<String, Object> data) {
				String difyTmpl = (String) data.get("answer");
				List<String> variables = new ArrayList<>();
				String tmpl = StringTemplateUtil.fromDifyTmpl(difyTmpl, variables);
				List<VariableSelector> inputs = variables.stream().map(variable -> {
					String[] splits = variable.split("\\.", 2);
					return new VariableSelector(splits[0], splits[1]);
				}).toList();
				return new AnswerNodeData(inputs, AnswerNodeData.DEFAULT_OUTPUTS).setAnswer(tmpl);
			}

			@Override
			public Map<String, Object> dump(AnswerNodeData nodeData) {
				Map<String, Object> data = new HashMap<>();
				String difyTmpl = StringTemplateUtil.toDifyTmpl(nodeData.getAnswer());
				data.put("answer", difyTmpl);
				return data;
			}
		}),

		CUSTOM(AbstractNodeDataConverter.defaultCustomDialectConverter(AnswerNodeData.class));

		private final DialectConverter<AnswerNodeData> dialectConverter;

		public DialectConverter<AnswerNodeData> dialectConverter() {
			return dialectConverter;
		}

		AnswerNodeDialectConverter(DialectConverter<AnswerNodeData> dialectConverter) {
			this.dialectConverter = dialectConverter;
		}

	}

}

package com.alibaba.cloud.ai.model.workflow.nodedata;

import com.alibaba.cloud.ai.model.Variable;
import com.alibaba.cloud.ai.model.VariableSelector;
import com.alibaba.cloud.ai.model.VariableType;
import com.alibaba.cloud.ai.model.workflow.NodeData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author HeYQ
 * @since 2024-12-12 21:26
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
public class QuestionClassifierNodeData extends NodeData {

	public static final Variable DEFAULT_OUTPUT_SCHEMA = new Variable("class_name", VariableType.STRING.value());

	private LLMNodeData.ModelConfig model;

	private LLMNodeData.MemoryConfig memoryConfig;

	private String instruction;

	private List<ClassConfig> classes;

	public QuestionClassifierNodeData(List<VariableSelector> inputs, List<Variable> outputs) {
		super(inputs, outputs);
	}

	@Data
	@AllArgsConstructor
	public static class ClassConfig {

		private String id;

		private String text;

	}

}

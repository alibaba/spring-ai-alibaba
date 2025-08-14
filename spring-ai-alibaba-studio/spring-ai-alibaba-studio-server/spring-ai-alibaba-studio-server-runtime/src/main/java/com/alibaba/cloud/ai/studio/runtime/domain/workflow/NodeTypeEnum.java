package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import lombok.Getter;

@Getter
public enum NodeTypeEnum {

	START("Start", "开始节点"), INPUT("Input", "输入节点"), OUTPUT("Output", "输出节点"),
	VARIABLE_ASSIGN("VariableAssign", "变量赋值节点"), VARIABLE_HANDLE("VariableHandle", "变量处理节点"),
	APP_CUSTOM("AppCustom", "自定义应用节点"), AGENT_GROUP("AgentGroup", "智能体组节点"), SCRIPT("Script", "脚本节点"),
	CLASSIFIER("Classifier", "问题分类节点"), LLM("LLM", "大模型节点"), COMPONENT("AppComponent", "应用组件节点"),
	JUDGE("Judge", "判断节点"), RETRIEVAL("Retrieval", "知识库节点"), API("API", "Api调用节点"), PLUGIN("Plugin", "插件节点"),
	MCP("MCP", "MCP节点"), PARAMETER_EXTRACTOR("ParameterExtractor", "参数提取节点"),
	ITERATOR_START("IteratorStart", "循环体开始节点"), ITERATOR("Iterator", "循环节点"), ITERATOR_END("IteratorEnd", "循环体结束节点"),
	PARALLEL_START("ParallelStart", "批处理开始节点"), PARALLEL("Parallel", "批处理节点"), PARALLEL_END("ParallelEnd", "批处理结束节点"),
	END("End", "结束节点");

	private final String code;

	private final String desc;

	NodeTypeEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

}

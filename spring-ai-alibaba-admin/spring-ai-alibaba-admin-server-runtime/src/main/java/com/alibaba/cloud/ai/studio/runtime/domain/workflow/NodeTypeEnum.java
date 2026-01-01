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
package com.alibaba.cloud.ai.studio.runtime.domain.workflow;

import lombok.Getter;

@Getter
public enum NodeTypeEnum {

	START("Start", "开始节�?), INPUT("Input", "输入节点"), OUTPUT("Output", "输出节点"),
	VARIABLE_ASSIGN("VariableAssign", "变量赋值节�?), VARIABLE_HANDLE("VariableHandle", "变量处理节点"),
	APP_CUSTOM("AppCustom", "自定义应用节�?), AGENT_GROUP("AgentGroup", "智能体组节点"), SCRIPT("Script", "脚本节点"),
	CLASSIFIER("Classifier", "问题分类节点"), LLM("LLM", "大模型节�?), COMPONENT("AppComponent", "应用组件节点"),
	JUDGE("Judge", "判断节点"), RETRIEVAL("Retrieval", "知识库节�?), API("API", "Api调用节点"), PLUGIN("Plugin", "插件节点"),
	MCP("MCP", "MCP节点"), PARAMETER_EXTRACTOR("ParameterExtractor", "参数提取节点"),
	ITERATOR_START("IteratorStart", "循环体开始节�?), ITERATOR("Iterator", "循环节点"), ITERATOR_END("IteratorEnd", "循环体结束节�?),
	PARALLEL_START("ParallelStart", "批处理开始节�?), PARALLEL("Parallel", "批处理节�?), PARALLEL_END("ParallelEnd", "批处理结束节�?),
	END("End", "结束节点");

	private final String code;

	private final String desc;

	NodeTypeEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

}

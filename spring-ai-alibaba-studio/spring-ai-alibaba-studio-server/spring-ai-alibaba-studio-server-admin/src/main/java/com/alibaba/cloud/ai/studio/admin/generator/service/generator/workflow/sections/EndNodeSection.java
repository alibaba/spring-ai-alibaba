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

package com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.sections;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.alibaba.cloud.ai.studio.admin.generator.model.VariableSelector;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.EndNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import org.springframework.stereotype.Component;

@Component
public class EndNodeSection implements NodeSection<EndNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.END.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		EndNodeData data = (EndNodeData) node.getData();
		String outputKey = data.getOutputKey();
		List<VariableSelector> selector = data.getInputs();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();
		sb.append("// EndNode [ ").append(id).append(" ]\n");

		String codeStr;
		if ("text".equalsIgnoreCase(data.getOutputType())) {
			// 如果输出类型为text，则使用对应的输出模板输出最终结果
			if (data.getTextTemplateVars().isEmpty()) {
				codeStr = String.format("state -> Map.of(\"%s\", %s)", data.getOutputKey(),
						ObjectToCodeUtil.toCode(data.getTextTemplate()));
			}
			else {
				codeStr = String.format("""
						state -> {
						    String template = %s;
						    Map<String, Object> params = %s.stream()
						            .collect(Collectors.toMap(
						                    key -> key,
						                    key -> state.value(key).orElse(""),
						                    (o1, o2) -> o2));
						    template = new PromptTemplate(template).render(params);
						    return Map.of("%s", template);
						}

						""", ObjectToCodeUtil.toCode(data.getTextTemplate()),
						ObjectToCodeUtil.toCode(data.getTextTemplateVars()), data.getOutputKey());
			}
		}
		else {
			codeStr = String.format("""
					state -> Map.of("%s", Map.of(%s))
					""", outputKey,
					selector.stream()
						.flatMap(v -> Stream.of(String.format("\"%s\"", v.getLabel()),
								String.format("state.value(\"%s\").orElse(\"\")", v.getNameInCode())))
						.collect(Collectors.joining(", ")));
		}

		// 最终节点用于输出用户选中的变量
		sb.append("stateGraph.addNode(\"")
			.append(varName)
			.append("\", AsyncNodeAction.node_async(")
			.append(codeStr)
			.append("));");
		sb.append(String.format("%n"));
		return sb.toString();
	}

	@Override
	public List<String> getImports() {
		return List.of("java.util.stream.Stream", "java.util.stream.Collectors",
				"org.springframework.ai.chat.prompt.PromptTemplate");
	}

}

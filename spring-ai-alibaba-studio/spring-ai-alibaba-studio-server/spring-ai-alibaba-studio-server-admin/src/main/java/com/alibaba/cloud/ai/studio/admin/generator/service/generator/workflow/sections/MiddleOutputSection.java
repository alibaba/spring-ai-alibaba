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

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.MiddleOutputNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;
import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MiddleOutputSection implements NodeSection<MiddleOutputNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.MIDDLE_OUTPUT.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		MiddleOutputNodeData nodeData = (MiddleOutputNodeData) node.getData();
		return String.format("""
				// -- MiddleOutputNode [%s] --
				stateGraph.addNode("%s", AsyncNodeAction.node_async(
				    createMiddleOutputNodeAction(%s, %s, %s))
				);

				""", varName, varName, ObjectToCodeUtil.toCode(nodeData.getOutputTemplate()),
				ObjectToCodeUtil.toCode(nodeData.getVarKeys()), ObjectToCodeUtil.toCode(nodeData.getOutputKey()));
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case STUDIO ->
				"""
						private NodeAction createMiddleOutputNodeAction(String outputTemplate, List<String> keys, String outputKey) {
						    return state -> {
						        Map<String, Object> params = keys.stream()
						                .collect(Collectors.toUnmodifiableMap(
						                        key -> key,
						                        key -> state.value(key).orElse(""),
						                        (a, b) -> b
						                ));
						        String output = new PromptTemplate(outputTemplate).render(params);
						        return Map.of(outputKey, output);
						    };
						}
						""";
			default -> NodeSection.super.assistMethodCode(dialectType);
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("java.util.stream.Collectors", "org.springframework.ai.chat.prompt.PromptTemplate");
	}

}

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
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.ListOperatorNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ListOperatorNodeSection implements NodeSection<ListOperatorNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.LIST_OPERATOR.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		ListOperatorNodeData d = (ListOperatorNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();
		String javaType = switch (d.getElementClassType()) {
			case STRING -> "String";
			case NUMBER -> "Number";
			case BOOLEAN -> "Boolean";
			default -> "Object";
		};

		sb.append(String.format("// —— ListOperatorNode [%s] ——%n", id));
		if (javaType.equals("Object")) {
			sb.append(String.format("// todo: define your own class and implement its comparator and filter.%n"));
		}
		sb.append(String.format(
				"ListOperatorNode<%s> %s = ListOperatorNode.<%s>builder().mode(ListOperatorNode.Mode.LIST)%n", javaType,
				varName, javaType));

		if (d.getInputKey() != null) {
			sb.append(String.format(".inputKey(\"%s\")%n", escape(d.getInputKey())));
		}

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		// 排序
		if (d.getOrder() != null) {
			if (javaType.equals("Number")) {
				if (d.getOrder() == ListOperatorNodeData.Ordered.ASC) {
					sb.append(String
						.format(".comparator((n1, n2) -> Double.compare(n1.doubleValue(), n2.doubleValue()))%n"));
				}
				else {
					sb.append(String
						.format(".comparator((n1, n2) -> Double.compare(n2.doubleValue(), n1.doubleValue()))%n"));
				}
			}
			else {
				if (d.getOrder() == ListOperatorNodeData.Ordered.ASC) {
					sb.append(String.format(".comparator(Comparator.naturalOrder())%n"));
				}
				else {
					sb.append(String.format(".comparator(Comparator.reverseOrder())%n"));
				}
			}
		}

		// 过滤
		if (d.getFilters() != null && !d.getFilters().isEmpty()) {
			ListOperatorNodeData.FilterCondition filterCondition = d.getFilters().get(0);
			String val = filterCondition.value();
			if (javaType.equals("String")) {
				val = "\"" + val + "\"";
			}
			String converted = filterCondition.condition().convert("x", val);
			sb.append(String.format(".filter(x -> %s)%n", converted));
		}

		// 限制数量
		if (d.getLimitNumber() != null) {
			sb.append(String.format(".limitNumber(%d)%n", d.getLimitNumber()));
		}

		if (d.getElementClassType() != null) {
			sb.append(String.format(".elementClassType(%s.class)%n", javaType));
		}

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName, varName));

		return sb.toString();
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.ListOperatorNode", "java.util.Comparator");
	}

}

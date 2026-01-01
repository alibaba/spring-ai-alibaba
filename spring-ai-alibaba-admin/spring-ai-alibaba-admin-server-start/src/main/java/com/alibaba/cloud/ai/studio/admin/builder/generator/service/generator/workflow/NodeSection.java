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
package com.alibaba.cloud.ai.studio.admin.builder.generator.service.generator.workflow;

import java.io.InputStream;
import java.util.List;
import java.util.function.Supplier;

import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Edge;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.builder.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.builder.generator.service.dsl.DSLDialectType;

/**
 * Render a node data
 *
 * @author robocanic
 * @since 2025/5/23
 */
// TODO: 将assistMethodCode生成的代码单独放在生成工程的一个类�?
public interface NodeSection<T extends NodeData> {

	boolean support(NodeType nodeType);

	// TODO: NodeData里有varName字段，去掉varName参数
	String render(Node node, String varName);

	/**
	 * 返回当前节点需要导入的类列�?
	 * @return 类列�?
	 */
	List<String> getImports();

	default String escape(String input) {
		if (input == null) {
			return "";
		}
		return input.replace("\\", "\\\\")
			.replace("\"", "\\\"")
			.replace("\n", "\\n")
			.replace("\r", "\\r")
			.replace("\t", "\\t");
	}

	/**
	 * 生成stateGraph边的代码。edge列表为从当前节点出发的边�?如果当前节点有条件边，则应重写本方法。本方法默认为无条件的边�?
	 * @param nodeData 当前节点（边起始节点）的数据
	 * @param edges 边列表，且边的source和handle应格式化为varName
	 * @return 生成的代�?
	 */
	default String renderEdges(T nodeData, List<Edge> edges) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("// Edges For [%s]%n", nodeData.getVarName()));
		if (edges.isEmpty()) {
			return "";
		}
		sb.append(String.format("stateGraph%n"));
		edges.forEach(
				edge -> sb.append(String.format(".addEdge(\"%s\", \"%s\")%n", edge.getSource(), edge.getTarget())));
		sb.append(String.format(";%n%n"));
		return sb.toString();
	}

	/**
	 * 当前类型节点的共用的辅助代码，这部分代码会生成在节点代码之前
	 * @param dialectType DSL类型
	 * @return 辅助代码
	 */
	default String assistMethodCode(DSLDialectType dialectType) {
		return "";
	}

	record ResourceFile(String fileName, Type type, Supplier<InputStream> inputStreamSupplier) {

		@Override
		public String toString() {
			return String.format("\"%s\"", switch (type()) {
				case CLASS_PATH -> "classpath:" + fileName();
				default -> fileName();
			});
		}
		public enum Type {

			LOCAL, URL, CLASS_PATH

		}
	}

	/**
	 * 部分节点需要额外添加的资源文件
	 * @param dialectType DSL 类型
	 * @param nodeData 节点数据
	 * @return 资源文件列表
	 */
	default List<ResourceFile> resourceFiles(DSLDialectType dialectType, T nodeData) {
		return List.of();
	}

}

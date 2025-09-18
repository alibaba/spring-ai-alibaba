/*
 * Copyright 2025 the original author or authors.
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
import java.util.Map;

import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.StreamHttpNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;

import org.springframework.stereotype.Component;

@Component
public class StreamHttpNodeSection implements NodeSection<StreamHttpNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.STREAM_HTTP.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		StreamHttpNodeData d = (StreamHttpNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— StreamHttpNode [%s] ——%n", id));
		sb.append(
				String.format("StreamHttpNodeParam.Builder %sParamBuilder = StreamHttpNodeParam.builder()%n", varName));
		sb.append(".webClient(WebClient.create())\n");

		// HTTP method
		if (d.getMethod() != null && !"GET".equals(d.getMethod())) {
			sb.append(String.format(".method(HttpMethod.%s)%n", d.getMethod().toUpperCase()));
		}

		// URL
		if (d.getUrl() != null) {
			sb.append(String.format(".url(\"%s\")%n", escape(d.getUrl())));
		}

		// Headers
		if (d.getHeaders() != null && !d.getHeaders().isEmpty()) {
			sb.append(".headers(Map.of(\n");
			boolean first = true;
			for (Map.Entry<String, String> entry : d.getHeaders().entrySet()) {
				if (!first) {
					sb.append(",\n");
				}
				sb.append(String.format("    \"%s\", \"%s\"", escape(entry.getKey()), escape(entry.getValue())));
				first = false;
			}
			sb.append("\n))\n");
		}

		// Stream format
		if (d.getStreamFormat() != null && !"SSE".equals(d.getStreamFormat())) {
			sb.append(String.format(".streamFormat(StreamHttpNodeParam.StreamFormat.%s)%n", d.getStreamFormat()));
		}

		// Stream mode
		if (d.getStreamMode() != null && !"DISTRIBUTE".equals(d.getStreamMode())) {
			sb.append(String.format(".streamMode(StreamHttpNodeParam.StreamMode.%s)%n", d.getStreamMode()));
		}

		// Delimiter
		if (d.getDelimiter() != null && !"\n".equals(d.getDelimiter())) {
			sb.append(String.format(".delimiter(\"%s\")%n", escape(d.getDelimiter())));
		}

		// Output key
		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		// Timeout
		if (d.getTimeout() != null && !d.getTimeout().equals(30000)) {
			sb.append(String.format(".readTimeout(Duration.ofMillis(%d))%n", d.getTimeout()));
		}

		sb.append(";\n");

		// Create StreamHttpNode
		sb.append(String.format("StreamHttpNode %s = new StreamHttpNode(%sParamBuilder.build());%n", varName, varName));

		// Add to state graph as async node since it's streaming
		String assistNodeCode = String.format("wrapperStreamHttpNodeAction(%s, \"%s\")", varName, varName);
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName,
				assistNodeCode));

		return sb.toString();
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> """
					 private NodeAction wrapperStreamHttpNodeAction(StreamHttpNode streamHttpNode, String varName) {
					     return state -> {
					         try {
					             Flux<Map<String, Object>> resultFlux = streamHttpNode.executeStreaming(state);
					             List<Map<String, Object>> results = resultFlux.collectList().block();

					             Map<String, Object> output = new HashMap<>();
					             if (results != null && !results.isEmpty()) {
					                 output.put(varName + "_data", results);
					                 output.put(varName + "_status", "success");
					                 output.put(varName + "_count", results.size());
					             } else {
					                 output.put(varName + "_data", Collections.emptyList());
					                 output.put(varName + "_status", "empty");
					                 output.put(varName + "_count", 0);
					             }
					             return output;
					         } catch (Exception e) {
					             return Map.of(
					                 varName + "_data", Collections.emptyList(),
					                 varName + "_status", "error",
					                 varName + "_error", e.getMessage()
					             );
					         }
					     };
					 }
					""";
			case STUDIO -> """
					 private NodeAction wrapperStreamHttpNodeAction(StreamHttpNode streamHttpNode, String varName) {
					     return state -> {
					         try {
					             Flux<Map<String, Object>> resultFlux = streamHttpNode.executeStreaming(state);
					             List<Map<String, Object>> results = resultFlux.collectList().block();

					             Map<String, Object> output = new HashMap<>();
					             if (results != null && !results.isEmpty()) {
					                 output.put("data", results);
					                 output.put("status", "success");
					             } else {
					                 output.put("data", Collections.emptyList());
					                 output.put("status", "empty");
					             }
					             return output;
					         } catch (Exception e) {
					             return Map.of(
					                 "data", Collections.emptyList(),
					                 "status", "error",
					                 "error", e.getMessage()
					             );
					         }
					     };
					 }
					""";
			default -> "";
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.StreamHttpNode",
				"com.alibaba.cloud.ai.graph.node.StreamHttpNodeParam",
				"org.springframework.web.reactive.function.client.WebClient", "org.springframework.http.HttpMethod",
				"reactor.core.publisher.Flux", "java.time.Duration", "java.util.Map", "java.util.HashMap",
				"java.util.List", "java.util.Collections");
	}

}

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
import java.util.Map;

import com.alibaba.cloud.ai.graph.node.HttpNode;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.Node;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata.HttpNodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.service.generator.workflow.NodeSection;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

// TODO: 支持失败时默认值以及异常分支、支持Studio的多字段输出模式
@Component
public class HttpNodeSection implements NodeSection<HttpNodeData> {

	@Override
	public boolean support(NodeType nodeType) {
		return NodeType.HTTP.equals(nodeType);
	}

	@Override
	public String render(Node node, String varName) {
		HttpNodeData d = (HttpNodeData) node.getData();
		String id = node.getId();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("// —— HttpNode [%s] ——%n", id));
		sb.append(String.format("HttpNode %s = HttpNode.builder()%n", varName));
		if (d.getMethod() != null && d.getMethod() != HttpMethod.GET) {
			sb.append(String.format(".method(HttpMethod.%s)%n", d.getMethod().name()));
		}

		if (d.getUrl() != null) {
			sb.append(String.format(".url(\"%s\")%n", escape(d.getUrl())));
		}

		for (Map.Entry<String, String> entry : d.getHeaders().entrySet()) {
			sb.append(String.format(".header(\"%s\", \"%s\")%n", escape(entry.getKey()), escape(entry.getValue())));
		}

		for (Map.Entry<String, String> entry : d.getQueryParams().entrySet()) {
			sb.append(String.format(".queryParam(\"%s\", \"%s\")%n", escape(entry.getKey()), escape(entry.getValue())));
		}

		if (d.getRawBodyMap() != null && !d.getRawBodyMap().isEmpty()
				&& !"none".equals(d.getRawBodyMap().get("type"))) {
			String rawJson;
			try {
				rawJson = new ObjectMapper().writeValueAsString(d.getRawBodyMap());
			}
			catch (JsonProcessingException e) {
				throw new RuntimeException("serialization Http body map failed", e);
			}
			sb.append(String.format(".body(HttpNode.HttpRequestNodeBody.fromJson(\"%s\"))%n", escape(rawJson)));
		}

		HttpNode.AuthConfig ac = d.getAuthConfig();
		if (ac != null) {
			if (ac.isBasic()) {
				sb.append(String.format(".auth(HttpNode.AuthConfig.basic(\"%s\", \"%s\"))%n", escape(ac.getUsername()),
						escape(ac.getPassword())));
			}
			else if (ac.isBearer()) {
				sb.append(String.format(".auth(HttpNode.AuthConfig.bearer(\"%s\"))%n", escape(ac.getToken())));
			}
		}

		HttpNode.RetryConfig rc = d.getRetryConfig();
		if (rc != null) {
			sb.append(String.format(".retryConfig(new HttpNode.RetryConfig(%d, %d, %b))%n", rc.getMaxRetries(),
					rc.getMaxRetryInterval(), rc.isEnable()));
		}

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		sb.append(".build();\n");

		// 辅助节点，用于转换HttpNode的结果
		String assistNodeCode = String.format("wrapperHttpNodeAction(%s, \"%s\")", varName, varName);
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", varName,
				assistNodeCode));

		return sb.toString();
	}

	@Override
	public String assistMethodCode(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> """
					 private NodeAction wrapperHttpNodeAction(NodeAction httpNodeAction, String varName) {
					     return state -> {
					         String key = varName + "_body";
					         Map<String, Object> result = httpNodeAction.apply(state);
					         Object object = result.get(key);
					         if(!(object instanceof Map<?, ?> map)) {
					             return Map.of();
					         }
					         return Map.of(varName + "_headers", map.get("headers"), key, map.get("body"),
					                 varName + "_status_code", map.get("status"));
					     };
					 }
					""";
			case STUDIO -> """
					 private NodeAction wrapperHttpNodeAction(NodeAction httpNodeAction, String varName) {
					     return state -> {
					         String key = varName + "_output";
					         Map<String, Object> result = httpNodeAction.apply(state);
					         Object object = result.get(key);
					         if(!(object instanceof Map<?, ?> map)) {
					             return Map.of();
					         }
					         return Map.of(key, map.get("body"));
					     };
					 }
					""";
			default -> "";
		};
	}

	@Override
	public List<String> getImports() {
		return List.of("com.alibaba.cloud.ai.graph.node.HttpNode", "org.springframework.http.HttpMethod");
	}

}

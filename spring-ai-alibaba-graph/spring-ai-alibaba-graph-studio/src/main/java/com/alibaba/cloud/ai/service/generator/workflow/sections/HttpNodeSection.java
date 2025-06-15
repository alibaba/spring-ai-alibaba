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

package com.alibaba.cloud.ai.service.generator.workflow.sections;

import com.alibaba.cloud.ai.graph.node.HttpNode;
import com.alibaba.cloud.ai.model.workflow.Node;
import com.alibaba.cloud.ai.model.workflow.NodeType;
import com.alibaba.cloud.ai.model.workflow.nodedata.HttpNodeData;
import com.alibaba.cloud.ai.service.generator.workflow.NodeSection;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HttpNodeSection implements NodeSection {

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

		HttpNode.HttpRequestNodeBody body = d.getBody();
		if (body != null && body.hasContent()) {
			sb.append(".body(HttpRequestNodeBody.from(/* raw body value */))\n");
		}

		HttpNode.AuthConfig ac = d.getAuthConfig();
		if (ac != null) {
			if (ac.isBasic()) {
				sb.append(String.format(".auth(AuthConfig.basic(\"%s\", \"%s\"))%n", escape(ac.getUsername()),
						escape(ac.getPassword())));
			}
			else if (ac.isBearer()) {
				sb.append(String.format(".auth(AuthConfig.bearer(\"%s\"))%n", escape(ac.getToken())));
			}
		}

		HttpNode.RetryConfig rc = d.getRetryConfig();
		if (rc != null) {
			sb.append(String.format(".retryConfig(new RetryConfig(%d, %d, %b))%n", rc.getMaxRetries(),
					rc.getMaxRetryInterval(), rc.isEnable()));
		}

		if (d.getOutputKey() != null) {
			sb.append(String.format(".outputKey(\"%s\")%n", escape(d.getOutputKey())));
		}

		sb.append(".build();\n");
		sb.append(String.format("stateGraph.addNode(\"%s\", AsyncNodeAction.node_async(%s));%n%n", id, varName));

		return sb.toString();
	}

}

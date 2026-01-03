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

package com.alibaba.cloud.ai.agent.nacos.vo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class McpServersVO {

	List<McpServerVO> mcpServers;

	public List<McpServerVO> getMcpServers() {
		return mcpServers;
	}

	public void setMcpServers(List<McpServerVO> mcpServers) {
		this.mcpServers = mcpServers;
	}

	public static class McpServerVO {

		String mcpServerName;

		String version;

		Set<String> whiteTools;

		Map<String, String> headers;

		Map<String, String> queryParams;

		public String getMcpServerName() {
			return mcpServerName;
		}

		public void setMcpServerName(String mcpServerName) {
			this.mcpServerName = mcpServerName;
		}

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}

		public Set<String> getWhiteTools() {
			return whiteTools;
		}

		public void setWhiteTools(Set<String> whiteTools) {
			this.whiteTools = whiteTools;
		}

		public Map<String, String> getHeaders() {
			return headers;
		}

		public void setHeaders(Map<String, String> headers) {
			this.headers = headers;
		}

		public Map<String, String> getQueryParams() {
			return queryParams;
		}

		public void setQueryParams(Map<String, String> queryParams) {
			this.queryParams = queryParams;
		}

	}

}



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
package com.alibaba.cloud.ai.example.manus.dynamic.mcp.model.vo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 内部服务器配置类
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class McpServerConfig {

	private String url;

	@JsonProperty("command")
	private String command;

	@JsonProperty("args")
	private List<String> args;

	@JsonProperty("env")
	private Map<String, String> env;

	public McpServerConfig() {
		this.env = new HashMap<>();
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public List<String> getArgs() {
		return args;
	}

	public void setArgs(List<String> args) {
		this.args = args;
	}

	public Map<String, String> getEnv() {
		return env;
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	/**
	 * 将ServerConfig转换为JSON字符串
	 * @return 转换后的JSON字符串
	 */
	public String toJson() {
		try {
			return new ObjectMapper().writeValueAsString(this);
		}
		catch (Exception e) {
			// 如果序列化失败，则手动构建简化版JSON
			StringBuilder sb = new StringBuilder();
			sb.append("{");

			// 添加URL（如果存在）
			if (url != null && !url.isEmpty()) {
				sb.append("\"url\":\"").append(url).append("\"");
			}

			// 添加命令（如果存在）
			if (command != null && !command.isEmpty()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("\"command\":\"").append(command).append("\"");
			}

			// 添加参数（如果存在）
			if (args != null && !args.isEmpty()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("\"args\":[");
				boolean first = true;
				for (String arg : args) {
					if (!first)
						sb.append(",");
					sb.append("\"").append(arg).append("\"");
					first = false;
				}
				sb.append("]");
			}

			// 添加环境变量（如果存在）
			if (env != null && !env.isEmpty()) {
				if (sb.length() > 1)
					sb.append(",");
				sb.append("\"env\":{");
				boolean first = true;
				for (Map.Entry<String, String> entry : env.entrySet()) {
					if (!first)
						sb.append(",");
					sb.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
					first = false;
				}
				sb.append("}");
			}

			sb.append("}");
			return sb.toString();
		}
	}

}

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
package com.alibaba.cloud.ai.toolcalling.searches;

import com.alibaba.cloud.ai.toolcalling.aliyunaisearch.AliyunAiSearchConstants;
import com.alibaba.cloud.ai.toolcalling.baidusearch.BaiduSearchConstants;
import com.alibaba.cloud.ai.toolcalling.serpapi.SerpApiConstants;
import com.alibaba.cloud.ai.toolcalling.tavily.TavilySearchConstants;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * List available services
 *
 * @author vlsmb
 */
public enum SearchEnum {

	TAVILY("tavily", TavilySearchConstants.TOOL_NAME), BAIDU("baidu", BaiduSearchConstants.TOOL_NAME),
	SERPAPI("serpapi", SerpApiConstants.TOOL_NAME), ALIYUN("aliyun", AliyunAiSearchConstants.TOOL_NAME);

	private final String name;

	private final String toolName;

	SearchEnum(String name, String toolName) {
		this.name = name;
		this.toolName = toolName;
	}

	@JsonValue
	public String getName() {
		return name;
	}

	public String getToolName() {
		return toolName;
	}

	@Override
	public String toString() {
		return "SearchEnum{" + "name='" + name + '\'' + ", toolName='" + toolName + '\'' + '}';
	}

	@JsonCreator
	public static SearchEnum fromName(String name) {
		for (SearchEnum searchEnum : SearchEnum.values()) {
			if (searchEnum.getName().equalsIgnoreCase(name)) {
				return searchEnum;
			}
		}
		throw new IllegalArgumentException("Invalid Search name: " + name);
	}

	public static SearchEnum fromToolName(String toolName) {
		for (SearchEnum searchEnum : SearchEnum.values()) {
			if (searchEnum.getToolName().equals(toolName)) {
				return searchEnum;
			}
		}
		throw new IllegalArgumentException("Invalid Search tool name: " + toolName);
	}

}

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

package com.alibaba.cloud.ai.example.deepresearch.enums;

/**
 * 流式节点前缀枚举。 用于标识流式输出节点的类型前缀，给前端用于展示
 */
public enum StreamNodePrefix {

	RESEARCHER_LLM_STREAM("researcher_llm_stream"), CODER_LLM_STREAM("coder_llm_stream"),
	REPORTER_LLM_STREAM("reporter_llm_stream");

	/** 节点前缀字符串 */
	private final String prefix;

	/**
	 * 构造方法。
	 * @param prefix 节点前缀字符串
	 */
	StreamNodePrefix(String prefix) {
		this.prefix = prefix;
	}

	/**
	 * 获取前缀字符串。
	 * @return 前缀
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * 判断给定节点名是否以任一枚举前缀开头。
	 * @param nodeName 节点名
	 * @return true-匹配，false-不匹配
	 */
	public static boolean matches(String nodeName) {
		for (StreamNodePrefix p : values()) {
			if (nodeName != null && nodeName.startsWith(p.prefix)) {
				return true;
			}
		}
		return false;
	}

}

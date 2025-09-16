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

package com.alibaba.cloud.ai.graph.node.code.entity;

/**
 * @author vlsmb
 * @since 2025/9/11
 * @param argName 参数在代码中对应的名称
 * @param value 参数值，如果为null，则从OverallState中获取
 * @param stateKey 参数在OverallState中的key，如果value不为null，则忽略stateKey
 */
public record CodeParam(String argName, Object value, String stateKey) {
	public CodeParam(String argName, String stateKey) {
		this(argName, null, stateKey);
	}

	public static CodeParam withValue(String argName, Object value) {
		return new CodeParam(argName, value, null);
	}

	public static CodeParam withKey(String argName, String stateKey) {
		return new CodeParam(argName, null, stateKey);
	}
}

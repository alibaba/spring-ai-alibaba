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

package com.alibaba.cloud.ai.example.graph.bigtool.agent;

import java.util.function.Function;

public class Tool {

	private String name;

	private String description;

	private Function<Object[], Object> function;

	private Class<?>[] parameterTypes;

	public Tool(String name, String description, Function<Object[], Object> function, Class<?>[] parameterTypes) {
		this.name = name;
		this.description = description;
		this.function = function;
		this.parameterTypes = parameterTypes;
	}

	public Object execute(Object... args) {
		return function.apply(args);
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public Class<?>[] getParameterTypes() {
		return parameterTypes;
	}

	public void setParameterTypes(Class<?>[] parameterTypes) {
		this.parameterTypes = parameterTypes;
	}

	@Override
	public String toString() {
		return "Tool{name='" + name + "', description='" + description + "'}";
	}

}

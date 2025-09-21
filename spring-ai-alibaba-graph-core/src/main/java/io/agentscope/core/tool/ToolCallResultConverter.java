/*
 * Copyright 2023-2025 the original author or authors.
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
package io.agentscope.core.tool;

import org.springframework.lang.Nullable;

import java.lang.reflect.Type;

@FunctionalInterface
public interface ToolCallResultConverter {

	/**
	 * Given an Object returned by a tool, convert it to a String compatible with the
	 * given class type.
	 */
	String convert(@Nullable Object result, @Nullable Type returnType);

}

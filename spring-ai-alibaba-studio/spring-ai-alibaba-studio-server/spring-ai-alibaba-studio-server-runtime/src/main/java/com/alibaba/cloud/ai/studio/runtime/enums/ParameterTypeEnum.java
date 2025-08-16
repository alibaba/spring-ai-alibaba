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
package com.alibaba.cloud.ai.studio.runtime.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Enumeration of parameter types supported in the system. Each type represents a
 * different data structure that can be used as a parameter.
 */
@Getter
public enum ParameterTypeEnum {

	STRING("String", "String type"), NUMBER("Number", "Number type"), BOOLEAN("Boolean", "Boolean type"),
	FILE("File", "File type"), ARRAY_STRING("Array<String>", "String array"),
	ARRAY_NUMBER("Array<Number>", "Number array"), ARRAY_BOOLEAN("Array<Boolean>", "Boolean array"),
	ARRAY_OBJECT("Array<Object>", "Object array"), ARRAY_FILE("Array<File>", "File array"),
	OBJECT("Object", "Object type"),;

	/** Type code used in the system */
	private final String code;

	/** Description of the parameter type */
	private final String desc;

	ParameterTypeEnum(String code, String desc) {
		this.code = code;
		this.desc = desc;
	}

	/**
	 * Retrieves all type codes defined in this enum
	 * @return Set of all type codes
	 */
	public static Set<String> getAllCodes() {
		return Arrays.stream(ParameterTypeEnum.values()).map(ParameterTypeEnum::getCode).collect(Collectors.toSet());
	}

}

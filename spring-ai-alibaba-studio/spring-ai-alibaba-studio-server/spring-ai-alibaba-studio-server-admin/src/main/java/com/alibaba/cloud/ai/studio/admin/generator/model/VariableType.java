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
package com.alibaba.cloud.ai.studio.admin.generator.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public enum VariableType {

	STRING("String", String.class, "string", "String"),

	NUMBER("Number", Number.class, "number", "Number"),

	BOOLEAN("Boolean", Boolean.class, "not supported", "Boolean"),

	OBJECT("Object", Object.class, "object", "Object"),

	// TODO：定义文件类型对象，以实现工作流直接使用文件
	FILE("File", Object.class, "file", "File"),

	ARRAY("Array", Object.class, "array", "Array"),

	ARRAY_BOOLEAN("Boolean[]", Boolean[].class, "array[boolean]", "Array<Boolean>"),

	ARRAY_STRING("String[]", String[].class, "array[string]", "Array<String>"),

	ARRAY_NUMBER("Number[]", Number[].class, "array[number]", "Array<Number>"),

	ARRAY_OBJECT("Object[]", Object[].class, "array[object]", "Array<Object>"),

	ARRAY_FILE("File[]", Object[].class, "file-list", "Array<FILE>");

	private final String value;

	private final Class<?> clazz;

	private final String difyValue;

	private final String studioValue;

	VariableType(String value, Class<?> clazz, String difyValue, String studioValue) {
		this.value = value;
		this.clazz = clazz;
		this.difyValue = difyValue;
		this.studioValue = studioValue;
	}

	public static List<VariableType> all() {
		return List.of(values());
	}

	public static List<VariableType> arrays() {
		return List.of(ARRAY_BOOLEAN, ARRAY_STRING, ARRAY_NUMBER, ARRAY_OBJECT, ARRAY_FILE, ARRAY);
	}

	public static List<VariableType> arraysWithOther(VariableType... other) {
		return Stream.concat(Stream.of(other), arrays().stream()).toList();
	}

	public static List<VariableType> except(VariableType... excepted) {
		return Stream.of(VariableType.values()).filter(type -> !Arrays.asList(excepted).contains(type)).toList();
	}

	public String value() {
		return value;
	}

	public Class<?> clazz() {
		return clazz;
	}

	public String difyValue() {
		return difyValue;
	}

	public String studioValue() {
		return studioValue;
	}

	public static Optional<VariableType> fromValue(String value) {
		return Arrays.stream(VariableType.values()).filter(type -> type.value.equals(value)).findFirst();
	}

	public static Optional<VariableType> fromDifyValue(String difyValue) {
		return Arrays.stream(VariableType.values()).filter(type -> type.difyValue.equals(difyValue)).findFirst();
	}

	public static Optional<VariableType> fromStudioValue(String studioValue) {
		return Arrays.stream(VariableType.values()).filter(type -> type.studioValue.equals(studioValue)).findFirst();
	}

}

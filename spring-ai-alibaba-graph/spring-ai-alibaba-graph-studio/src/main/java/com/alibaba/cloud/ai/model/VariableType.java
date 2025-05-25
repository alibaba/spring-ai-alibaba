package com.alibaba.cloud.ai.model;

import java.util.Arrays;
import java.util.Optional;

public enum VariableType {

	STRING("String", String.class, "string"),

	NUMBER("Number", Number.class, "number"),

	BOOLEAN("Boolean", Boolean.class, "not supported"),

	OBJECT("Object", Object.class, "object"),

	// FIXME find appropriate type
	FILE("File", Object.class, "file"),

	ARRAY_STRING("String[]", String[].class, "array[string]"),

	ARRAY_NUMBER("Number[]", Number[].class, "array[number]"),

	ARRAY_OBJECT("Object[]", Object[].class, "array[object]"),

	ARRAY_FILE("File[]", Object[].class, "file-list");

	private String value;

	private Class clazz;

	private String difyValue;

	VariableType(String value, Class clazz, String difyValue) {
		this.value = value;
		this.clazz = clazz;
		this.difyValue = difyValue;
	}

	public String value() {
		return value;
	}

	public Class clazz() {
		return clazz;
	}

	public String difyValue() {
		return difyValue;
	}

	public static Optional<VariableType> fromValue(String value) {
		return Arrays.stream(VariableType.values()).filter(type -> type.value.equals(value)).findFirst();
	}

	public static Optional<VariableType> fromDifyValue(String difyValue) {
		return Arrays.stream(VariableType.values()).filter(type -> type.difyValue.equals(difyValue)).findFirst();
	}

}

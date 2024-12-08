package com.alibaba.cloud.ai.model;

public enum VariableType {

	STRING("STRING", String.class, "string"),

	NUMBER("DOUBLE", Number.class, "number"),

	BOOLEAN("BOOLEAN", Boolean.class, "not supported"),

	OBJECT("OBJECT", Object.class, "object"),

	// FIXME find appropriate type
	FILE("FILE", Object.class, "file"),

	ARRAY_STRING("ARRAY_STRING", String[].class, "array[string]"),

	ARRAY_NUMBER("ARRAY_NUMBER", Number[].class, "array[number]"),

	ARRAY_OBJECT("ARRAY_OBJECT", Object[].class, "array[object]"),

	ARRAY_FILE("ARRAY_FILE", Object[].class, "file-list");

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

	public static VariableType difyValueOf(String difyValue) {
		for (VariableType type : VariableType.values()) {
			if (type.difyValue.equals(difyValue)) {
				return type;
			}
		}
		return null;
	}

}

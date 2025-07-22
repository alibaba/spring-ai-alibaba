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

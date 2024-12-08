package com.alibaba.cloud.ai.model;

import lombok.Data;

import java.util.Map;

@Data
public class Variable {

	private String name;

	private String value;

	private String valueType;

	private String description;

	private Map<String, Object> extraProperties;

	// only name and value is required
	public Variable(String name, String valueType) {
		this.name = name;
		this.valueType = valueType;
	}

}

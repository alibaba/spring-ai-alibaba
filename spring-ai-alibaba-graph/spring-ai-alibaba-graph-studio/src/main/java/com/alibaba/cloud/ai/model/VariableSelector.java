package com.alibaba.cloud.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@AllArgsConstructor
public class VariableSelector {

	private String namespace;

	private String name;

	// optional
	private String label;

	public VariableSelector(String namespace, String name) {
		this.namespace = namespace;
		this.name = name;
	}

}

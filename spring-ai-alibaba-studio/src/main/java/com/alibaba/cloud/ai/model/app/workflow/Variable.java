package com.alibaba.cloud.ai.model.app.workflow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
public class Variable {

	private String id;

	private String name;

	private String value;

	private String valueType;

	private String description;

	private List<String> selector;

}

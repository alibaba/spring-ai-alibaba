package com.alibaba.cloud.ai.schema;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ColumnDTO {

	private String name;

	private String description;

	private int enumeration;

	private String range;

	private String type;

	private List<String> samples;

	private List<String> data;

	private Map<String, String> mapping;

}

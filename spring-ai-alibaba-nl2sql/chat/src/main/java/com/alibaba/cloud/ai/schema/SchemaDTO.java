package com.alibaba.cloud.ai.schema;

import lombok.Data;

import java.util.List;

@Data
public class SchemaDTO {

	private String name;

	private String description;

	private Integer tableCount;

	private List<TableDTO> table;

	private List<List<String>> foreignKeys;

}
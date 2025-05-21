package com.alibaba.cloud.ai.schema;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TableDTO {

	private String name;

	private String description;

	private List<ColumnDTO> column = new ArrayList<ColumnDTO>();

	private List<String> primaryKeys;

}

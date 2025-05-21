package com.alibaba.cloud.ai.dbconnector.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnInfoBO extends DdlBaseBO {

	private String name;

	private String tableName;

	private String description;

	private String type;

	private boolean primary;

	private boolean notnull;

	private String samples;

}

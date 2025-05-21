package com.alibaba.cloud.ai.dbconnector.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableInfoBO extends DdlBaseBO {

	private String schema;

	private String name;

	private String description;

	private String type;

	private String foreignKey;

	private String primaryKey;

}

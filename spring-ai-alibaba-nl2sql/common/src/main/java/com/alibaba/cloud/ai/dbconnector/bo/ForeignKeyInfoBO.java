package com.alibaba.cloud.ai.dbconnector.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForeignKeyInfoBO extends DdlBaseBO {

	private String table;

	private String column;

	private String referencedTable;

	private String referencedColumn;

}

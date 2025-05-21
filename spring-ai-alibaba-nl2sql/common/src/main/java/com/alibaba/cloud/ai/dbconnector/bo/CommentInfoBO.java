package com.alibaba.cloud.ai.dbconnector.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentInfoBO extends DdlBaseBO {

	private String schema;

	private String table;

	private String column;

	private String description;

}

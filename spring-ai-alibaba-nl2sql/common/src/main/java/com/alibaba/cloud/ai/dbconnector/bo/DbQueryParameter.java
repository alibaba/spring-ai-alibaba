package com.alibaba.cloud.ai.dbconnector.bo;

import com.alibaba.cloud.ai.dbconnector.DbConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DbQueryParameter {

	private String aliuid;

	private String workspaceId;

	private String region;

	private String secretArn;

	private String dbInstanceId;

	private String database;

	private String schema;

	private String table;

	private String tablePattern;

	private List<String> tables;

	private String column;

	private String sql;

	public static DbQueryParameter from(DbConfig config) {
		DbQueryParameter param = new DbQueryParameter();
		BeanUtils.copyProperties(config, param);
		return param;
	}

}
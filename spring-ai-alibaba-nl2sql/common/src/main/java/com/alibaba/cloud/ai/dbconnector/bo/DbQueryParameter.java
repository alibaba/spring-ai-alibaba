/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

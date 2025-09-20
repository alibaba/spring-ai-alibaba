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
package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.connector.accessor.Accessor;
import com.alibaba.cloud.ai.connector.bo.ColumnInfoBO;
import com.alibaba.cloud.ai.connector.bo.DbQueryParameter;
import com.alibaba.cloud.ai.connector.bo.ForeignKeyInfoBO;
import com.alibaba.cloud.ai.connector.bo.ResultSetBO;
import com.alibaba.cloud.ai.connector.bo.TableInfoBO;
import com.alibaba.cloud.ai.connector.config.DbConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2数据库集成测试类 用于验证H2数据库配置和初始化脚本是否正确
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".*")
public class H2AccessorIntegrationTest {

	@Autowired
	private Accessor dbAccessor;

	@Autowired
	private DbConfig dbConfig;

	@Test
	public void testShowTables() throws Exception {
		DbQueryParameter queryParam = DbQueryParameter.from(dbConfig);
		queryParam.setSchema("public");

		List<TableInfoBO> publicTableInfoBOS = dbAccessor.showTables(dbConfig, queryParam);

		assertThat(publicTableInfoBOS).isNotEmpty();

		queryParam.setSchema("product_db");
		List<TableInfoBO> productTableInfoBOS = dbAccessor.showTables(dbConfig, queryParam);

		assertThat(productTableInfoBOS).isNotEmpty();
	}

	@Test
	public void testFetchTables() throws Exception {
		DbQueryParameter queryParam = DbQueryParameter.from(dbConfig);
		queryParam.setSchema("public");
		queryParam.setTables(List.of("datasource"));

		List<TableInfoBO> publicTableInfoBOS = dbAccessor.fetchTables(dbConfig, queryParam);

		assertThat(publicTableInfoBOS).isNotEmpty();

		queryParam.setSchema("product_db");
		queryParam.setTables(List.of("orders"));
		List<TableInfoBO> productTableInfoBOS = dbAccessor.fetchTables(dbConfig, queryParam);

		assertThat(productTableInfoBOS).isNotEmpty();
	}

	@Test
	public void testShowColumns() throws Exception {
		DbQueryParameter queryParam = DbQueryParameter.from(dbConfig);
		queryParam.setSchema("public");
		queryParam.setTable("datasource");

		List<ColumnInfoBO> publicColumnInfoBOS = dbAccessor.showColumns(dbConfig, queryParam);

		assertThat(publicColumnInfoBOS).isNotEmpty();

		queryParam.setSchema("product_db");
		queryParam.setTable("orders");
		List<ColumnInfoBO> productColumnInfoBOS = dbAccessor.showColumns(dbConfig, queryParam);

		assertThat(productColumnInfoBOS).isNotEmpty();
	}

	@Test
	public void testShowForeignKeys() throws Exception {
		DbQueryParameter queryParam = DbQueryParameter.from(dbConfig);
		queryParam.setSchema("public");
		queryParam.setTables(List.of("agent_datasource", "agent"));

		List<ForeignKeyInfoBO> publicForeignKeyInfoBOS = dbAccessor.showForeignKeys(dbConfig, queryParam);

		assertThat(publicForeignKeyInfoBOS).isNotEmpty();

		queryParam.setSchema("product_db");
		queryParam.setTables(List.of("order_items", "orders"));
		List<ForeignKeyInfoBO> productForeignKeyInfoBOS = dbAccessor.showForeignKeys(dbConfig, queryParam);

		assertThat(productForeignKeyInfoBOS).isNotEmpty();
	}

	@Test
	public void testSampleColumn() throws Exception {
		DbQueryParameter queryParam = DbQueryParameter.from(dbConfig);
		queryParam.setSchema("public");
		queryParam.setTable("datasource");
		queryParam.setColumn("name");

		List<String> publicForeignKeyInfoBOS = dbAccessor.sampleColumn(dbConfig, queryParam);

		assertThat(publicForeignKeyInfoBOS).isNotEmpty();

		queryParam.setSchema("product_db");
		queryParam.setTable("orders");
		queryParam.setColumn("status");
		List<String> productForeignKeyInfoBOS = dbAccessor.sampleColumn(dbConfig, queryParam);

		assertThat(productForeignKeyInfoBOS).isNotEmpty();
	}

	@Test
	public void testExecuteSqlAndReturnObject() throws Exception {
		DbQueryParameter queryParam = DbQueryParameter.from(dbConfig);
		queryParam.setSchema("public");
		queryParam.setSql("select count(*) from datasource");

		ResultSetBO publicResultSetBO = dbAccessor.executeSqlAndReturnObject(dbConfig, queryParam);

		assertThat(publicResultSetBO).isNotNull();
		assertThat(publicResultSetBO.getData()).isNotEmpty();

		queryParam.setSchema("product_db");
		queryParam.setSql("select count(*) from orders");
		ResultSetBO productResultSetBO = dbAccessor.executeSqlAndReturnObject(dbConfig, queryParam);

		assertThat(productResultSetBO).isNotNull();
		assertThat(productResultSetBO.getData()).isNotEmpty();
	}

}

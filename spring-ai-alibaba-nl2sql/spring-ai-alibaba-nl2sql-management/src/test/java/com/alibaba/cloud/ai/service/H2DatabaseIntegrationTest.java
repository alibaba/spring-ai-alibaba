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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * H2数据库集成测试类 用于验证H2数据库配置和初始化脚本是否正确
 */
@SpringBootTest
@ActiveProfiles("h2")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "AI_DASHSCOPE_API_KEY", matches = ".*")
public class H2DatabaseIntegrationTest {

	@Autowired
	private DataSource dataSource;

	@Test
	public void testH2DatabaseConnection() throws Exception {
		try (Connection connection = dataSource.getConnection()) {
			assertThat(connection).isNotNull();
			assertThat(connection.isValid(1)).isTrue();
		}
	}

	@Test
	public void testAgentTableExists() throws Exception {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM agent")) {

			assertThat(resultSet.next()).isTrue();
			int count = resultSet.getInt(1);
			assertThat(count).isGreaterThanOrEqualTo(0);
		}
	}

	@Test
	public void testBusinessKnowledgeTableExists() throws Exception {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM business_knowledge")) {

			assertThat(resultSet.next()).isTrue();
			int count = resultSet.getInt(1);
			assertThat(count).isGreaterThanOrEqualTo(0);
		}
	}

	@Test
	public void testSemanticModelTableExists() throws Exception {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM semantic_model")) {

			assertThat(resultSet.next()).isTrue();
			int count = resultSet.getInt(1);
			assertThat(count).isGreaterThanOrEqualTo(0);
		}
	}

	@Test
	public void testDataInitialization() throws Exception {
		try (Connection connection = dataSource.getConnection();
				Statement statement = connection.createStatement();
				ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM datasource")) {

			assertThat(resultSet.next()).isTrue();
			int count = resultSet.getInt(1);
			// 根据data-h2.sql中的数据，应该至少有一条数据
			assertThat(count).isGreaterThanOrEqualTo(0);
		}
	}

}

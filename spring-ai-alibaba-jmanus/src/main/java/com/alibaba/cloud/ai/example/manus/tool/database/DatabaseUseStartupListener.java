/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.manus.tool.database;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class DatabaseUseStartupListener implements ApplicationListener<ApplicationStartedEvent> {

	private static final Logger log = LoggerFactory.getLogger(DatabaseUseStartupListener.class);

	@Autowired
	private Environment environment;

	@Autowired
	private DataSourceService dataSourceService;

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		initializeDatabaseConfigs();
	}

	private void initializeDatabaseConfigs() {
		try {
			log.info("Starting to initialize database configurations...");

			// 使用配置解析工具类
			DatabaseConfigParser configParser = new DatabaseConfigParser(environment);
			Map<String, Map<String, String>> datasourceConfigs = configParser.parseDatasourceConfigs();

			if (datasourceConfigs.isEmpty()) {
				log.warn("No database configurations found. This is normal if no datasources are configured.");
				// 输出空的摘要信息
				printDatasourceSummary(new HashMap<>());
				return;
			}

			// 遍历并初始化每个数据源
			for (Map.Entry<String, Map<String, String>> entry : datasourceConfigs.entrySet()) {
				String datasourceName = entry.getKey();
				Map<String, String> config = entry.getValue();
				initializeDatasource(datasourceName, config);
			}

			log.info("Database configurations initialized with {} datasources", dataSourceService.getDataSourceCount());

			// 输出数据源清单
			printDatasourceSummary(datasourceConfigs);

		}
		catch (Exception e) {
			log.error("Failed to initialize database configurations", e);
		}
	}

	private void initializeDatasource(String datasourceName, Map<String, String> config) {
		try {
			String type = config.get(DatabaseConfigConstants.PROP_TYPE);
			String enable = config.get(DatabaseConfigConstants.PROP_ENABLE);
			String url = config.get(DatabaseConfigConstants.PROP_URL);
			String driverClassName = config.get(DatabaseConfigConstants.PROP_DRIVER_CLASS_NAME);
			String username = config.get(DatabaseConfigConstants.PROP_USERNAME);
			String password = config.get(DatabaseConfigConstants.PROP_PASSWORD);

			if (type == null || url == null || driverClassName == null) {
				log.warn("Incomplete configuration for datasource '{}'", datasourceName);
				return;
			}

			if (!DatabaseConfigConstants.ENABLE_TRUE.equals(enable)) {
				log.info("Datasource '{}' is disabled (enable: {})", datasourceName, enable);
				return;
			}

			// 创建数据源
			dataSourceService.addDataSource(datasourceName, url, username, password, driverClassName, type);
			log.info("Initialized datasource '{}' (type: {})", datasourceName, type);

		}
		catch (Exception e) {
			log.error("Failed to initialize datasource '{}'", datasourceName, e);
		}
	}

	private void printDatasourceSummary(Map<String, Map<String, String>> datasourceConfigs) {
		StringBuilder summary = new StringBuilder();
		summary.append("\n");
		summary.append("=".repeat(100)).append("\n");
		summary.append("DATABASE DATASOURCE SUMMARY").append("\n");
		summary.append("=".repeat(100)).append("\n");

		int totalConfigs = datasourceConfigs.size();
		int initializedCount = 0;
		int disabledCount = 0;

		for (Map.Entry<String, Map<String, String>> entry : datasourceConfigs.entrySet()) {
			String datasourceName = entry.getKey();
			Map<String, String> config = entry.getValue();

			String type = config.get(DatabaseConfigConstants.PROP_TYPE);
			String enable = config.get(DatabaseConfigConstants.PROP_ENABLE);
			String url = config.get(DatabaseConfigConstants.PROP_URL);

			boolean isEnabled = DatabaseConfigConstants.ENABLE_TRUE.equals(enable);
			boolean isInitialized = dataSourceService.hasDataSource(datasourceName);

			String status = isEnabled && isInitialized ? "✓ INSTANTIATED"
					: isEnabled && !isInitialized ? "✗ FAILED" : "○ DISABLED";

			summary.append(String.format("│ Datasource: %-12s │ Type: %-8s │ Status: %-12s │ URL: %-35s │\n",
					datasourceName, type != null ? type : "N/A", status, url != null ? url : "N/A"));

			if (isEnabled && isInitialized) {
				initializedCount++;
			}
			else if (!isEnabled) {
				disabledCount++;
			}
		}

		summary.append("-".repeat(100)).append("\n");
		summary.append(String.format("│ SUMMARY: Total=%d, Instantiated=%d, Disabled=%d, Failed=%d │\n", totalConfigs,
				initializedCount, disabledCount, totalConfigs - initializedCount - disabledCount));
		summary.append("=".repeat(100)).append("\n");
		summary.append("\n");

		log.info(summary.toString());
	}

}

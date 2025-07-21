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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * 数据库配置解析工具类
 */
public class DatabaseConfigParser {

	private static final Logger log = LoggerFactory.getLogger(DatabaseConfigParser.class);

	private final Environment environment;

	public DatabaseConfigParser(Environment environment) {
		this.environment = environment;
	}

	/**
	 * 发现所有数据源名称
	 */
	public Set<String> discoverDatasourceNames() {
		Set<String> names = new HashSet<>();

		try {
			// 通过扫描所有配置键来发现数据源
			Set<String> allKeys = getAllPropertyKeys();

			for (String key : allKeys) {
				// 匹配模式：database.tool.datasource.{datasourceName}.type
				if (isDatasourceTypeProperty(key)) {
					String datasourceName = extractDatasourceName(key);
					if (datasourceName != null) {
						names.add(datasourceName);
						log.debug("Discovered datasource: {}", datasourceName);
					}
				}
			}

		}
		catch (Exception e) {
			log.error("Failed to discover datasource names dynamically", e);
			// 不抛出异常，而是返回空集合，让调用方处理
		}

		return names;
	}

	/**
	 * 获取所有配置键
	 */
	private Set<String> getAllPropertyKeys() {
		Set<String> keys = new HashSet<>();

		try {
			if (environment instanceof org.springframework.core.env.ConfigurableEnvironment) {
				org.springframework.core.env.ConfigurableEnvironment configEnv = (org.springframework.core.env.ConfigurableEnvironment) environment;
				for (org.springframework.core.env.PropertySource<?> propertySource : configEnv.getPropertySources()) {
					if (propertySource instanceof org.springframework.core.env.MapPropertySource) {
						org.springframework.core.env.MapPropertySource mapSource = (org.springframework.core.env.MapPropertySource) propertySource;
						keys.addAll(mapSource.getSource().keySet());
					}
					else if (propertySource instanceof org.springframework.core.env.EnumerablePropertySource) {
						org.springframework.core.env.EnumerablePropertySource<?> enumSource = (org.springframework.core.env.EnumerablePropertySource<?>) propertySource;
						for (String key : enumSource.getPropertyNames()) {
							keys.add(key);
						}
					}
				}
			}
		}
		catch (Exception e) {
			log.debug("Could not extract all property keys: {}", e.getMessage());
		}

		return keys;
	}

	/**
	 * 判断是否为数据源类型属性
	 */
	private boolean isDatasourceTypeProperty(String key) {
		return key.startsWith(DatabaseConfigConstants.CONFIG_PREFIX)
				&& key.endsWith("." + DatabaseConfigConstants.PROP_TYPE);
	}

	/**
	 * 从配置键中提取数据源名称
	 */
	private String extractDatasourceName(String key) {
		try {
			// 移除前缀和后缀，提取中间的数据源名称
			String prefix = DatabaseConfigConstants.CONFIG_PREFIX;
			String suffix = "." + DatabaseConfigConstants.PROP_TYPE;

			if (key.startsWith(prefix) && key.endsWith(suffix)) {
				String middle = key.substring(prefix.length(), key.length() - suffix.length());
				// 确保中间部分不为空且不包含额外的点号
				if (!middle.isEmpty() && !middle.contains(".")) {
					return middle;
				}
			}
		}
		catch (Exception e) {
			log.debug("Failed to extract datasource name from key: {}", key);
		}

		return null;
	}

	/**
	 * 解析数据源配置
	 */
	public Map<String, Map<String, String>> parseDatasourceConfigs() {
		Map<String, Map<String, String>> configs = new HashMap<>();

		Set<String> datasourceNames = discoverDatasourceNames();

		for (String name : datasourceNames) {
			String prefix = DatabaseConfigConstants.CONFIG_PREFIX + name;

			String type = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_TYPE);
			String enable = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_ENABLE);
			String url = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_URL);
			String driverClassName = environment
				.getProperty(prefix + "." + DatabaseConfigConstants.PROP_DRIVER_CLASS_NAME);
			String username = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_USERNAME);
			String password = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_PASSWORD);

			if (type != null && url != null && driverClassName != null) {
				Map<String, String> config = new HashMap<>();
				config.put(DatabaseConfigConstants.PROP_TYPE, type);
				config.put(DatabaseConfigConstants.PROP_ENABLE, enable);
				config.put(DatabaseConfigConstants.PROP_URL, url);
				config.put(DatabaseConfigConstants.PROP_DRIVER_CLASS_NAME, driverClassName);
				config.put(DatabaseConfigConstants.PROP_USERNAME, username);
				config.put(DatabaseConfigConstants.PROP_PASSWORD, password);

				configs.put(name, config);
				log.debug("Found datasource config: {}", name);
			}
		}

		return configs;
	}

	/**
	 * 获取单个数据源配置
	 */
	public Map<String, String> getDatasourceConfig(String datasourceName) {
		String prefix = DatabaseConfigConstants.CONFIG_PREFIX + datasourceName;

		String type = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_TYPE);
		String enable = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_ENABLE);
		String url = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_URL);
		String driverClassName = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_DRIVER_CLASS_NAME);
		String username = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_USERNAME);
		String password = environment.getProperty(prefix + "." + DatabaseConfigConstants.PROP_PASSWORD);

		Map<String, String> config = new HashMap<>();
		config.put(DatabaseConfigConstants.PROP_TYPE, type);
		config.put(DatabaseConfigConstants.PROP_ENABLE, enable);
		config.put(DatabaseConfigConstants.PROP_URL, url);
		config.put(DatabaseConfigConstants.PROP_DRIVER_CLASS_NAME, driverClassName);
		config.put(DatabaseConfigConstants.PROP_USERNAME, username);
		config.put(DatabaseConfigConstants.PROP_PASSWORD, password);

		return config;
	}

}

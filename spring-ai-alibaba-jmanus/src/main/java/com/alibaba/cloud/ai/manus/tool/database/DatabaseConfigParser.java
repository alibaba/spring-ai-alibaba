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

package com.alibaba.cloud.ai.manus.tool.database;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Database configuration parsing utility class
 */
public class DatabaseConfigParser {

	private static final Logger log = LoggerFactory.getLogger(DatabaseConfigParser.class);

	private final Environment environment;

	public DatabaseConfigParser(Environment environment) {
		this.environment = environment;
	}

	/**
	 * Discover all data source names
	 */
	public Set<String> discoverDatasourceNames() {
		Set<String> names = new HashSet<>();

		try {
			// Discover data sources by scanning all configuration keys
			Set<String> allKeys = getAllPropertyKeys();

			for (String key : allKeys) {
				// Match pattern: database.tool.datasource.{datasourceName}.type
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
			// Don't throw exception, return empty set and let caller handle
		}

		return names;
	}

	/**
	 * Get all configuration keys
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
	 * Determine if it's a data source type property
	 */
	private boolean isDatasourceTypeProperty(String key) {
		return key.startsWith(DatabaseConfigConstants.CONFIG_PREFIX)
				&& key.endsWith("." + DatabaseConfigConstants.PROP_TYPE);
	}

	/**
	 * Extract data source name from configuration key
	 */
	private String extractDatasourceName(String key) {
		try {
			// Remove prefix and suffix, extract middle data source name
			String prefix = DatabaseConfigConstants.CONFIG_PREFIX;
			String suffix = "." + DatabaseConfigConstants.PROP_TYPE;

			if (key.startsWith(prefix) && key.endsWith(suffix)) {
				String middle = key.substring(prefix.length(), key.length() - suffix.length());
				// Ensure middle part is not empty and doesn't contain extra dots
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
	 * Parse data source configuration
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
	 * Get single data source configuration
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

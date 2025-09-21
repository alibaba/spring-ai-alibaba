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
package com.alibaba.cloud.ai.manus.config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cloud.ai.manus.config.entity.ConfigEntity;
import com.alibaba.cloud.ai.manus.config.repository.ConfigRepository;

@Service
public class ConfigService implements IConfigService, ApplicationListener<ContextRefreshedEvent> {

	private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

	@Autowired
	private ConfigRepository configRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	private final Map<String, ConfigCacheEntry<String>> configCache = new ConcurrentHashMap<>();

	private boolean initialized = false;

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (!initialized) {
			initialized = true;
			init();
		}
	}

	private void init() {
		// Only get beans with @ConfigurationProperties annotation
		Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
		log.info("Found {} configuration beans", configBeans.size());

		// Initialize each configuration bean
		configBeans.values().forEach(this::initializeConfig);
	}

	private void initializeConfig(Object bean) {
		// Collect all valid config paths from the bean
		Set<String> validConfigPaths = Arrays.stream(bean.getClass().getDeclaredFields())
			.filter(field -> field.isAnnotationPresent(ConfigProperty.class))
			.map(field -> field.getAnnotation(ConfigProperty.class).path())
			.collect(Collectors.toSet());

		// Remove obsolete configurations that are no longer defined in ManusProperties
		if (bean instanceof ManusProperties) {
			log.info("Cleaning up obsolete configurations not defined in ManusProperties...");
			List<ConfigEntity> allConfigs = configRepository.findAll();
			List<ConfigEntity> obsoleteConfigs = allConfigs.stream()
				.filter(config -> !validConfigPaths.contains(config.getConfigPath()))
				.collect(Collectors.toList());

			if (!obsoleteConfigs.isEmpty()) {
				log.info("Found {} obsolete configurations to remove:", obsoleteConfigs.size());
				obsoleteConfigs.forEach(config -> {
					log.info("  - Removing obsolete config: {} ({})", config.getConfigPath(), config.getDescription());
					configRepository.delete(config);
					// Remove from cache as well
					configCache.remove(config.getConfigPath());
				});
				log.info("✅ Obsolete configuration cleanup completed");
			}
			else {
				log.info("✅ No obsolete configurations found");
			}
		}

		// Initialize/update configurations defined in the bean
		Arrays.stream(bean.getClass().getDeclaredFields())
			.filter(field -> field.isAnnotationPresent(ConfigProperty.class))
			.forEach(field -> {
				ConfigProperty annotation = field.getAnnotation(ConfigProperty.class);
				String configPath = annotation.path();

				// Check if configuration already exists
				if (!configRepository.existsByConfigPath(configPath)) {
					// Create new configuration entity
					ConfigEntity entity = new ConfigEntity();
					entity.setConfigGroup(annotation.group());
					entity.setConfigSubGroup(annotation.subGroup());
					entity.setConfigKey(annotation.key());
					entity.setConfigPath(configPath);
					entity.setDescription(annotation.description());
					entity.setDefaultValue(annotation.defaultValue());
					entity.setInputType(annotation.inputType());

					// Try to get configuration value from environment
					String value = environment.getProperty(configPath);
					if (value != null) {
						entity.setConfigValue(value);
					}
					else {
						entity.setConfigValue(annotation.defaultValue());
					}

					// If it's SELECT type, save options JSON
					if (annotation.inputType().name().equals("SELECT") && annotation.options().length > 0) {
						// Convert options to JSON string
						ConfigOption[] options = annotation.options();
						StringBuilder optionsJson = new StringBuilder("[");
						for (int i = 0; i < options.length; i++) {
							if (i > 0)
								optionsJson.append(",");
							optionsJson.append("{")
								.append("\"value\":\"")
								.append(options[i].value())
								.append("\",")
								.append("\"label\":\"")
								.append(options[i].label())
								.append("\"")
								.append("}");
						}
						optionsJson.append("]");
						entity.setOptionsJson(optionsJson.toString());
					}

					// Save configuration
					log.debug("Creating new config: {}", configPath);
					configRepository.save(entity);

					// Set field value
					setFieldValue(bean, field, entity.getConfigValue());
				}
			});
	}

	public String getConfigValue(String configPath) {
		// Check cache
		ConfigCacheEntry<String> cacheEntry = configCache.get(configPath);
		if (cacheEntry != null && !cacheEntry.isExpired()) {
			return cacheEntry.getValue();
		}

		// If cache doesn't exist or is expired, get from database
		Optional<ConfigEntity> configOpt = configRepository.findByConfigPath(configPath);
		if (configOpt.isPresent()) {
			String value = configOpt.get().getConfigValue();
			configCache.put(configPath, new ConfigCacheEntry<>(value));
			return value;
		}
		return null;
	}

	@Transactional
	public void updateConfig(String configPath, String newValue) {
		ConfigEntity entity = configRepository.findByConfigPath(configPath)
			.orElseThrow(() -> new IllegalArgumentException("Config not found: " + configPath));

		entity.setConfigValue(newValue);
		configRepository.save(entity);

		// Update cache
		configCache.put(configPath, new ConfigCacheEntry<>(newValue));

		// Update all beans using this configuration
		Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
		configBeans.values().forEach(bean -> updateBeanConfig(bean, configPath, newValue));
	}

	private void updateBeanConfig(Object bean, String configPath, String newValue) {
		Arrays.stream(bean.getClass().getDeclaredFields())
			.filter(field -> field.isAnnotationPresent(ConfigProperty.class))
			.filter(field -> field.getAnnotation(ConfigProperty.class).path().equals(configPath))
			.forEach(field -> setFieldValue(bean, field, newValue));
	}

	private void setFieldValue(Object bean, Field field, String value) {
		try {
			field.setAccessible(true);

			// Convert value based on field type
			Object convertedValue = convertValue(value, field.getType());
			field.set(bean, convertedValue);

		}
		catch (IllegalAccessException e) {
			log.error("Failed to set field value", e);
		}
	}

	private Object convertValue(String value, Class<?> targetType) {
		if (value == null)
			return null;

		if (targetType == String.class) {
			return value;
		}
		else if (targetType == Boolean.class || targetType == boolean.class) {
			if ("on".equalsIgnoreCase(value))
				return Boolean.TRUE;
			return Boolean.valueOf(value);
		}
		else if (targetType == Integer.class || targetType == int.class) {
			return Integer.valueOf(value);
		}
		else if (targetType == Long.class || targetType == long.class) {
			return Long.valueOf(value);
		}
		else if (targetType == Double.class || targetType == double.class) {
			return Double.valueOf(value);
		}

		throw new IllegalArgumentException("Unsupported type: " + targetType);
	}

	public List<ConfigEntity> getAllConfigs() {
		return configRepository.findAll();
	}

	public Optional<ConfigEntity> getConfig(String configPath) {
		return configRepository.findByConfigPath(configPath);
	}

	public void resetConfig(String configPath) {
		ConfigEntity entity = configRepository.findByConfigPath(configPath)
			.orElseThrow(() -> new IllegalArgumentException("Config not found: " + configPath));

		entity.setConfigValue(entity.getDefaultValue());
		configRepository.save(entity);

		// Update all beans using this configuration
		Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
		configBeans.values().forEach(bean -> updateBeanConfig(bean, configPath, entity.getDefaultValue()));
	}

	/**
	 * Get configuration items by configuration group name
	 * @param groupName Configuration group name
	 * @return All configuration items in this group
	 */
	public List<ConfigEntity> getConfigsByGroup(String groupName) {
		return configRepository.findByConfigGroup(groupName);
	}

	/**
	 * Batch update configuration items
	 * @param configs List of configuration items to update
	 */
	@Transactional
	public void batchUpdateConfigs(List<ConfigEntity> configs) {
		for (ConfigEntity config : configs) {
			ConfigEntity existingConfig = configRepository.findById(config.getId())
				.orElseThrow(() -> new IllegalArgumentException("Config not found with ID: " + config.getId()));

			// Only update configuration value
			existingConfig.setConfigValue(config.getConfigValue());
			configRepository.save(existingConfig);

			// Update all beans using this configuration
			Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
			configBeans.values()
				.forEach(bean -> updateBeanConfig(bean, existingConfig.getConfigPath(),
						existingConfig.getConfigValue()));
		}
	}

	/**
	 * Reset all configurations to their default values
	 */
	@Transactional
	public void resetAllConfigsToDefaults() {
		List<ConfigEntity> allConfigs = configRepository.findAll();

		for (ConfigEntity config : allConfigs) {
			if (config.getDefaultValue() != null && !config.getDefaultValue().equals(config.getConfigValue())) {
				config.setConfigValue(config.getDefaultValue());
				configRepository.save(config);

				// Update all beans using this configuration
				Map<String, Object> configBeans = applicationContext
					.getBeansWithAnnotation(ConfigurationProperties.class);
				configBeans.values()
					.forEach(bean -> updateBeanConfig(bean, config.getConfigPath(), config.getDefaultValue()));
			}
		}
	}

}

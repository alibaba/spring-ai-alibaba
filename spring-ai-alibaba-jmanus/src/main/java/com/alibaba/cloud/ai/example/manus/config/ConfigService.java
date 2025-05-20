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
package com.alibaba.cloud.ai.example.manus.config;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigEntity;
import com.alibaba.cloud.ai.example.manus.config.repository.ConfigRepository;

import jakarta.annotation.PostConstruct;

@Service
public class ConfigService {

	private static final Logger log = LoggerFactory.getLogger(ConfigService.class);

	@Autowired
	private ConfigRepository configRepository;

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	private Environment environment;

	private final Map<String, ConfigCacheEntry<String>> configCache = new ConcurrentHashMap<>();

	@PostConstruct
	public void init() {
		// 只获取带有@ConfigurationProperties注解的Bean
		Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
		log.info("Found {} configuration beans", configBeans.size());

		// 初始化每个配置Bean
		configBeans.values().forEach(this::initializeConfig);
	}

	private void initializeConfig(Object bean) {
		Arrays.stream(bean.getClass().getDeclaredFields())
			.filter(field -> field.isAnnotationPresent(ConfigProperty.class))
			.forEach(field -> {
				ConfigProperty annotation = field.getAnnotation(ConfigProperty.class);
				String configPath = annotation.path();

				// 检查配置是否已存在
				if (!configRepository.existsByConfigPath(configPath)) {
					// 创建新的配置实体
					ConfigEntity entity = new ConfigEntity();
					entity.setConfigGroup(annotation.group());
					entity.setConfigSubGroup(annotation.subGroup());
					entity.setConfigKey(annotation.key());
					entity.setConfigPath(configPath);
					entity.setDescription(annotation.description());
					entity.setDefaultValue(annotation.defaultValue());
					entity.setInputType(annotation.inputType());

					// 尝试从环境中获取配置值
					String value = environment.getProperty(configPath);
					if (value != null) {
						entity.setConfigValue(value);
					}
					else {
						entity.setConfigValue(annotation.defaultValue());
					}

					// 如果是SELECT类型，保存选项JSON
					if (annotation.inputType().name().equals("SELECT") && annotation.options().length > 0) {
						// 将选项转换为JSON字符串
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

					// 保存配置
					log.debug("Creating new config: {}", configPath);
					configRepository.save(entity);

					// 设置字段值
					setFieldValue(bean, field, entity.getConfigValue());
				}
			});
	}

	public String getConfigValue(String configPath) {
		// 检查缓存
		ConfigCacheEntry<String> cacheEntry = configCache.get(configPath);
		if (cacheEntry != null && !cacheEntry.isExpired()) {
			return cacheEntry.getValue();
		}

		// 如果缓存不存在或已过期，从数据库获取
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

		// 更新缓存
		configCache.put(configPath, new ConfigCacheEntry<>(newValue));

		// 更新所有使用此配置的Bean
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

			// 根据字段类型转换值
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

		// 更新所有使用此配置的Bean
		Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
		configBeans.values().forEach(bean -> updateBeanConfig(bean, configPath, entity.getDefaultValue()));
	}

	/**
	 * 根据配置组名获取配置项
	 * @param groupName 配置组名
	 * @return 该组的所有配置项
	 */
	public List<ConfigEntity> getConfigsByGroup(String groupName) {
		return configRepository.findByConfigGroup(groupName);
	}

	/**
	 * 批量更新配置项
	 * @param configs 需要更新的配置项列表
	 */
	@Transactional
	public void batchUpdateConfigs(List<ConfigEntity> configs) {
		for (ConfigEntity config : configs) {
			ConfigEntity existingConfig = configRepository.findById(config.getId())
				.orElseThrow(() -> new IllegalArgumentException("Config not found with ID: " + config.getId()));

			// 只更新配置值
			existingConfig.setConfigValue(config.getConfigValue());
			configRepository.save(existingConfig);

			// 更新所有使用此配置的Bean
			Map<String, Object> configBeans = applicationContext.getBeansWithAnnotation(ConfigurationProperties.class);
			configBeans.values()
				.forEach(bean -> updateBeanConfig(bean, existingConfig.getConfigPath(),
						existingConfig.getConfigValue()));
		}
	}

}

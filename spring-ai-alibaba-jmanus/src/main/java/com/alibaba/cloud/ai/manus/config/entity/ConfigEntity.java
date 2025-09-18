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
package com.alibaba.cloud.ai.manus.config.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "system_config")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Configuration group
	 */
	@Column(nullable = false)
	private String configGroup;

	/**
	 * Configuration sub-group
	 */
	@Column(nullable = false)
	private String configSubGroup;

	/**
	 * Configuration key
	 */
	@Column(nullable = false)
	private String configKey;

	/**
	 * Configuration item full path
	 */
	@Column(nullable = false, unique = true)
	private String configPath;

	/**
	 * Configuration value
	 */
	@Column(columnDefinition = "TEXT")
	private String configValue;

	/**
	 * Default value
	 */
	@Column(columnDefinition = "TEXT")
	private String defaultValue;

	/**
	 * Configuration description
	 */
	@Column(columnDefinition = "TEXT")
	private String description;

	/**
	 * Input type
	 */
	@Column(nullable = false)
	@Enumerated(EnumType.STRING)
	private ConfigInputType inputType;

	/**
	 * Options JSON string for storing SELECT type option data
	 */
	@Column(columnDefinition = "TEXT")
	private String optionsJson;

	/**
	 * Last update time
	 */
	@Column(nullable = false)
	private LocalDateTime updateTime;

	/**
	 * Create time
	 */
	@Column(nullable = false)
	private LocalDateTime createTime;

	@PrePersist
	protected void onCreate() {
		createTime = LocalDateTime.now();
		updateTime = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updateTime = LocalDateTime.now();
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getConfigGroup() {
		return configGroup;
	}

	public void setConfigGroup(String configGroup) {
		this.configGroup = configGroup;
	}

	public String getConfigSubGroup() {
		return configSubGroup;
	}

	public void setConfigSubGroup(String configSubGroup) {
		this.configSubGroup = configSubGroup;
	}

	public String getConfigKey() {
		return configKey;
	}

	public void setConfigKey(String configKey) {
		this.configKey = configKey;
	}

	public String getConfigPath() {
		return configPath;
	}

	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	public String getConfigValue() {
		return configValue;
	}

	public void setConfigValue(String configValue) {
		this.configValue = configValue;
	}

	public String getDefaultValue() {
		return defaultValue;
	}

	public void setDefaultValue(String defaultValue) {
		this.defaultValue = defaultValue;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ConfigInputType getInputType() {
		return inputType;
	}

	public void setInputType(ConfigInputType inputType) {
		this.inputType = inputType;
	}

	public String getOptionsJson() {
		return optionsJson;
	}

	public void setOptionsJson(String optionsJson) {
		this.optionsJson = optionsJson;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

}

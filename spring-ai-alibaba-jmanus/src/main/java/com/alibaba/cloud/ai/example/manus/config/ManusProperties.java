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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigInputType;

@Component
@ConfigurationProperties(prefix = "manus")
public class ManusProperties {

	@Lazy
	@Autowired
	private ConfigService configService;

	@ConfigProperty(group = "manus", subGroup = "browser", key = "headless", path = "manus.browser.headless",
			description = "是否使用无头浏览器模式", defaultValue = "false", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "是"), @ConfigOption(value = "false", label = "否") })
	private volatile Boolean browserHeadless;

	public Boolean getBrowserHeadless() {
		String configPath = "manus.browser.headless";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			browserHeadless = Boolean.valueOf(value);
		}
		return browserHeadless;
	}

	public void setBrowserHeadless(Boolean browserHeadless) {
		this.browserHeadless = browserHeadless;
	}

	@ConfigProperty(group = "manus", subGroup = "interaction", key = "openBrowser", path = "manus.openBrowserAuto",
			description = "启动时自动打开浏览器", defaultValue = "true", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "是"), @ConfigOption(value = "false", label = "否") })
	private volatile Boolean openBrowserAuto;

	public Boolean getOpenBrowserAuto() {
		String configPath = "manus.openBrowserAuto";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			openBrowserAuto = Boolean.valueOf(value);
		}
		return openBrowserAuto;
	}

	public void setOpenBrowserAuto(Boolean openBrowserAuto) {
		this.openBrowserAuto = openBrowserAuto;
	}

	@ConfigProperty(group = "manus", subGroup = "interaction", key = "consoleQuery", path = "manus.consoleQuery",
			description = "启用控制台交互模式", defaultValue = "false", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "是"), @ConfigOption(value = "false", label = "否") })
	private volatile Boolean consoleQuery;

	public Boolean getConsoleQuery() {
		String configPath = "manus.consoleQuery";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			consoleQuery = Boolean.valueOf(value);
		}
		return consoleQuery;
	}

	public void setConsoleQuery(Boolean consoleQuery) {
		this.consoleQuery = consoleQuery;
	}

	@ConfigProperty(group = "manus", subGroup = "browser", key = "requestTimeout",
			path = "manus.browser.requestTimeout", description = "浏览器请求超时时间(秒)", defaultValue = "180",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer browserRequestTimeout;

	@ConfigProperty(group = "manus", subGroup = "agent", key = "maxSteps", path = "manus.maxSteps",
			description = "智能体执行最大步数", defaultValue = "20", inputType = ConfigInputType.NUMBER)
	private volatile Integer maxSteps;

	public Integer getBrowserRequestTimeout() {
		String configPath = "manus.browser.requestTimeout";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			browserRequestTimeout = Integer.valueOf(value);
		}
		return browserRequestTimeout;
	}

	public void setBrowserRequestTimeout(Integer browserRequestTimeout) {
		this.browserRequestTimeout = browserRequestTimeout;
	}

	public Integer getMaxSteps() {
		String configPath = "manus.maxSteps";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			maxSteps = Integer.valueOf(value);
		}
		return maxSteps;
	}

	public void setMaxSteps(Integer maxSteps) {
		this.maxSteps = maxSteps;
	}

	@ConfigProperty(group = "manus", subGroup = "agents", key = "forceOverrideFromYaml", path = "manus.agents.forceOverrideFromYaml",
			description = "强制使用YAML配置文件覆盖同名Agent", defaultValue = "false", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "是"), @ConfigOption(value = "false", label = "否") })
	private volatile Boolean forceOverrideFromYaml;

	public Boolean getForceOverrideFromYaml() {
		String configPath = "manus.agents.forceOverrideFromYaml";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			forceOverrideFromYaml = Boolean.valueOf(value);
		}
		return forceOverrideFromYaml;
	}

	public void setForceOverrideFromYaml(Boolean forceOverrideFromYaml) {
		this.forceOverrideFromYaml = forceOverrideFromYaml;
	}

	@ConfigProperty(group = "manus", subGroup = "general", key = "baseDir", path = "manus.baseDir",
			description = "manus根目录", defaultValue = "", inputType = ConfigInputType.TEXT)
	private volatile String baseDir = "";

	@ConfigProperty(group = "manus", subGroup = "general", key = "debugDetail", path = "manus.general.debugDetail",
			description = "debug模式 ：会要求模型输出更多内容，方便查找问题，但速度更慢", defaultValue = "false", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "是"), @ConfigOption(value = "false", label = "否") })
	private volatile Boolean debugDetail;

	public String getBaseDir() {
		String configPath = "manus.baseDir";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			baseDir = value;
		}
		return baseDir;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public Boolean getDebugDetail() {
		String configPath = "manus.general.debugDetail";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			debugDetail = Boolean.valueOf(value);
		}
		return debugDetail;
	}

	public void setDebugDetail(Boolean debugDetail) {
		this.debugDetail = debugDetail;
	}

	@ConfigProperty(group = "manus", subGroup = "agent", key = "userInputTimeout",
			path = "manus.agent.userInputTimeout", description = "用户输入表单等待超时时间(秒)", defaultValue = "300",
			inputType = ConfigInputType.NUMBER)
	private volatile Integer userInputTimeout;

	public Integer getUserInputTimeout() {
		String configPath = "manus.agent.userInputTimeout";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			userInputTimeout = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (userInputTimeout == null) {
			// Attempt to parse the default value specified in the annotation,
			// or use a hardcoded default if parsing fails or is complex to retrieve here.
			// For simplicity, directly using the intended default.
			userInputTimeout = 300;
		}
		return userInputTimeout;
	}

	public void setUserInputTimeout(Integer userInputTimeout) {
		this.userInputTimeout = userInputTimeout;
	}

	// Infinite Context SubGroup
	@ConfigProperty(group = "manus", subGroup = "infiniteContext", key = "enabled", path = "manus.infiniteContext.enabled",
			description = "是否开启无限上下文", defaultValue = "false", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "是"), @ConfigOption(value = "false", label = "否") })
	private volatile Boolean infiniteContextEnabled;

	public Boolean getInfiniteContextEnabled() {
		String configPath = "manus.infiniteContext.enabled";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			infiniteContextEnabled = Boolean.valueOf(value);
		}
		return infiniteContextEnabled;
	}

	public void setInfiniteContextEnabled(Boolean infiniteContextEnabled) {
		this.infiniteContextEnabled = infiniteContextEnabled;
	}

	@ConfigProperty(group = "manus", subGroup = "infiniteContext", key = "parallelThreads", path = "manus.infiniteContext.parallelThreads",
			description = "并行处理线程数", defaultValue = "4", inputType = ConfigInputType.NUMBER)
	private volatile Integer infiniteContextParallelThreads;

	public Integer getInfiniteContextParallelThreads() {
		String configPath = "manus.infiniteContext.parallelThreads";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			infiniteContextParallelThreads = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (infiniteContextParallelThreads == null) {
			infiniteContextParallelThreads = 4;
		}
		return infiniteContextParallelThreads;
	}

	public void setInfiniteContextParallelThreads(Integer infiniteContextParallelThreads) {
		this.infiniteContextParallelThreads = infiniteContextParallelThreads;
	}

	@ConfigProperty(group = "manus", subGroup = "infiniteContext", key = "taskContextSize", path = "manus.infiniteContext.taskContextSize",
			description = "单个任务的处理的上下文配置大小", defaultValue = "8192", inputType = ConfigInputType.NUMBER)
	private volatile Integer infiniteContextTaskContextSize;

	public Integer getInfiniteContextTaskContextSize() {
		String configPath = "manus.infiniteContext.taskContextSize";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			infiniteContextTaskContextSize = Integer.valueOf(value);
		}
		// Ensure a default value if not configured and not set
		if (infiniteContextTaskContextSize == null) {
			infiniteContextTaskContextSize = 8192;
		}
		return infiniteContextTaskContextSize;
	}

	public void setInfiniteContextTaskContextSize(Integer infiniteContextTaskContextSize) {
		this.infiniteContextTaskContextSize = infiniteContextTaskContextSize;
	}

	// File System Security SubGroup
	@ConfigProperty(group = "manus", subGroup = "filesystem", key = "allowExternalAccess", path = "manus.filesystem.allowExternalAccess",
			description = "Whether to allow file operations outside the working directory", defaultValue = "false", inputType = ConfigInputType.CHECKBOX,
			options = { @ConfigOption(value = "true", label = "Yes"), @ConfigOption(value = "false", label = "No") })
	private volatile Boolean allowExternalAccess;

	public Boolean getAllowExternalAccess() {
		String configPath = "manus.filesystem.allowExternalAccess";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			allowExternalAccess = Boolean.valueOf(value);
		}
		// Default to false for security
		if (allowExternalAccess == null) {
			allowExternalAccess = false;
		}
		return allowExternalAccess;
	}

	public void setAllowExternalAccess(Boolean allowExternalAccess) {
		this.allowExternalAccess = allowExternalAccess;
	}

}

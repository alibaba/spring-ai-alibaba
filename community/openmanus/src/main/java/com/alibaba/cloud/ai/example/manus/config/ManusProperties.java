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

	@ConfigProperty(group = "browser", subGroup = "settings", key = "headless", path = "manus.browser.headless",
			description = "是否使用无头浏览器模式", defaultValue = "true", inputType = ConfigInputType.SELECT,
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

	@ConfigProperty(group = "interaction", subGroup = "settings", key = "openBrowser", path = "manus.openBrowserAuto",
			description = "启动时自动打开浏览器", defaultValue = "true", inputType = ConfigInputType.SELECT,
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

	@ConfigProperty(group = "interaction", subGroup = "settings", key = "consoleQuery", path = "manus.consoleQuery",
			description = "启用控制台交互模式", defaultValue = "false", inputType = ConfigInputType.SELECT,
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

	@ConfigProperty(group = "agent", subGroup = "settings", key = "maxSteps", path = "manus.maxSteps",
			description = "智能体执行最大步数", defaultValue = "20", inputType = ConfigInputType.NUMBER)
	private volatile Integer maxSteps;

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

	@ConfigProperty(group = "agent", subGroup = "settings", key = "resetAgents", path = "manus.resetAgents",
			description = "重置所有agent", defaultValue = "true", inputType = ConfigInputType.SELECT,
			options = { @ConfigOption(value = "true", label = "是"), @ConfigOption(value = "false", label = "否") })
	private volatile Boolean resetAgents;

	public Boolean getResetAgents() {
		String configPath = "manus.resetAgents";
		String value = configService.getConfigValue(configPath);
		if (value != null) {
			resetAgents = Boolean.valueOf(value);
		}
		return resetAgents;
	}

	public void setResetAgents(Boolean resetAgents) {
		this.resetAgents = resetAgents;
	}

}

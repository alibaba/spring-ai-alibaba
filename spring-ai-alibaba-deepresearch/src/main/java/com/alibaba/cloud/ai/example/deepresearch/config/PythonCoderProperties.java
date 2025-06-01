package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Python Coder Config
 *
 * @author vlsmb
 */
@ConfigurationProperties(prefix = DeepResearchProperties.PREFIX + ".python-coder")
public class PythonCoderProperties {

	/**
	 * Naming prefix when temporarily enabling Docker containers
	 */
	String containNamePrefix = "python-coder";

	/**
	 * Memory size limit
	 */
	String limitMemory = "500M";

	/**
	 * Container CPU core limit
	 */
	String cpuCore = "1";

	/**
	 * Enable/disable container network access
	 */
	boolean enableNetwork = false;

	/**
	 * Timeout of python code
	 */
	String codeTimeout = "60s";

	public String getContainNamePrefix() {
		return containNamePrefix;
	}

	public void setContainNamePrefix(String containNamePrefix) {
		this.containNamePrefix = containNamePrefix;
	}

	public String getLimitMemory() {
		return limitMemory;
	}

	public void setLimitMemory(String limitMemory) {
		this.limitMemory = limitMemory;
	}

	public String getCpuCore() {
		return cpuCore;
	}

	public void setCpuCore(String cpuCore) {
		this.cpuCore = cpuCore;
	}

	public boolean isEnableNetwork() {
		return enableNetwork;
	}

	public void setEnableNetwork(boolean enableNetwork) {
		this.enableNetwork = enableNetwork;
	}

	public String getCodeTimeout() {
		return codeTimeout;
	}

	public void setCodeTimeout(String codeTimeout) {
		this.codeTimeout = codeTimeout;
	}

}

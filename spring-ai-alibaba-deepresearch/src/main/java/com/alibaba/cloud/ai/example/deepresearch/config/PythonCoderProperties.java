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

package com.alibaba.cloud.ai.example.deepresearch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Python Coder Config
 *
 * @author vlsmb
 */
@ConfigurationProperties(prefix = PythonCoderProperties.PYTHON_CODER_PREFIX)
public class PythonCoderProperties {

	public static final String PYTHON_CODER_PREFIX = DeepResearchProperties.PREFIX + ".python-coder";

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

	/**
	 * The image of container. You can customize the image as long as it includes python3
	 * and pip3
	 */
	String imageName = "python:3-slim";

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

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

}

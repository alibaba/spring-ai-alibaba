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
	 * Docker Host Addr
	 */
	String dockerHost = "unix:///var/run/docker.sock";

	/**
	 * Naming prefix when temporarily enabling Docker containers
	 */
	String containNamePrefix = "python-coder";

	/**
	 * Memory size limit (MB)
	 */
	Long limitMemory = 500L;

	/**
	 * Container CPU core limit
	 */
	Long cpuCore = 1L;

	/**
	 * Enable/disable container network access
	 */
	boolean enableNetwork = false;

	/**
	 * Timeout of python code
	 */
	String codeTimeout = "60s";

	/**
	 * Timeout of Docker (s)
	 */
	Long dockerTimeout = 3000L;

	/**
	 * The image of container. You can customize the image as long as it includes python3
	 * and pip3
	 */
	String imageName = "python:3-slim";

	public String getDockerHost() {
		return dockerHost;
	}

	public void setDockerHost(String dockerHost) {
		this.dockerHost = dockerHost;
	}

	public String getContainNamePrefix() {
		return containNamePrefix;
	}

	public void setContainNamePrefix(String containNamePrefix) {
		this.containNamePrefix = containNamePrefix;
	}

	public Long getLimitMemory() {
		return limitMemory;
	}

	public void setLimitMemory(Long limitMemory) {
		this.limitMemory = limitMemory;
	}

	public Long getCpuCore() {
		return cpuCore;
	}

	public void setCpuCore(Long cpuCore) {
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

	public Long getDockerTimeout() {
		return dockerTimeout;
	}

	public void setDockerTimeout(Long dockerTimeout) {
		this.dockerTimeout = dockerTimeout;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

}

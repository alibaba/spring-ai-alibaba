/*
 * Copyright 2024-2025 the original author or authors.
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

package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.service.code.CodePoolExecutorEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author vlsmb
 * @since 2025/7/12
 */
@ConfigurationProperties(prefix = CodeExecutorProperties.CONFIG_PREFIX)
public class CodeExecutorProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.nl2sql.code-executor";

	/**
	 * Specify implementation class of code container pool runtime service
	 */
	CodePoolExecutorEnum codePoolExecutor = CodePoolExecutorEnum.DOCKER;

	/**
	 * Service host, use default address if null
	 */
	String host = null;

	/**
	 * Image name, can customize image with common third-party dependencies to replace
	 * this configuration
	 */
	String imageName = "continuumio/anaconda3:latest";

	/**
	 * Container name prefix
	 */
	String containerNamePrefix = "nl2sql-python-exec-";

	/**
	 * Task blocking queue size
	 */
	Integer taskQueueSize = 5;

	/**
	 * Maximum number of core containers
	 */
	Integer coreContainerNum = 2;

	/**
	 * Maximum number of temporary containers
	 */
	Integer tempContainerNum = 2;

	/**
	 * Core thread count of thread pool
	 */
	Integer coreThreadSize = 5;

	/**
	 * Maximum thread count of thread pool
	 */
	Integer maxThreadSize = 5;

	/**
	 * Survival time of temporary containers, in minutes
	 */
	Integer tempContainerAliveTime = 5;

	/**
	 * Task survival time of thread pool, in seconds
	 */
	Long keepThreadAliveTime = 60L;

	/**
	 * Task blocking queue size of thread pool
	 */
	Integer threadQueueSize = 10;

	/**
	 * Maximum container memory, in MB
	 */
	Long limitMemory = 500L;

	/**
	 * Number of container CPU cores
	 */
	Long cpuCore = 1L;

	/**
	 * Python code execution time limit
	 */
	String codeTimeout = "60s";

	/**
	 * Maximum container runtime
	 */
	Long containerTimeout = 3000L;

	/**
	 * Container network mode
	 */
	String networkMode = "none";

	public CodePoolExecutorEnum getCodePoolExecutor() {
		return codePoolExecutor;
	}

	public void setCodePoolExecutor(CodePoolExecutorEnum codePoolExecutor) {
		this.codePoolExecutor = codePoolExecutor;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getImageName() {
		return imageName;
	}

	public void setImageName(String imageName) {
		this.imageName = imageName;
	}

	public String getContainerNamePrefix() {
		return containerNamePrefix;
	}

	public void setContainerNamePrefix(String containerNamePrefix) {
		this.containerNamePrefix = containerNamePrefix;
	}

	public Integer getTaskQueueSize() {
		return taskQueueSize;
	}

	public void setTaskQueueSize(Integer taskQueueSize) {
		this.taskQueueSize = taskQueueSize;
	}

	public Integer getCoreContainerNum() {
		return coreContainerNum;
	}

	public void setCoreContainerNum(Integer coreContainerNum) {
		this.coreContainerNum = coreContainerNum;
	}

	public Integer getTempContainerNum() {
		return tempContainerNum;
	}

	public void setTempContainerNum(Integer tempContainerNum) {
		this.tempContainerNum = tempContainerNum;
	}

	public Integer getCoreThreadSize() {
		return coreThreadSize;
	}

	public void setCoreThreadSize(Integer coreThreadSize) {
		this.coreThreadSize = coreThreadSize;
	}

	public Integer getMaxThreadSize() {
		return maxThreadSize;
	}

	public void setMaxThreadSize(Integer maxThreadSize) {
		this.maxThreadSize = maxThreadSize;
	}

	public Integer getTempContainerAliveTime() {
		return tempContainerAliveTime;
	}

	public void setTempContainerAliveTime(Integer tempContainerAliveTime) {
		this.tempContainerAliveTime = tempContainerAliveTime;
	}

	public Long getKeepThreadAliveTime() {
		return keepThreadAliveTime;
	}

	public void setKeepThreadAliveTime(Long keepThreadAliveTime) {
		this.keepThreadAliveTime = keepThreadAliveTime;
	}

	public Integer getThreadQueueSize() {
		return threadQueueSize;
	}

	public void setThreadQueueSize(Integer threadQueueSize) {
		this.threadQueueSize = threadQueueSize;
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

	public String getCodeTimeout() {
		return codeTimeout;
	}

	public void setCodeTimeout(String codeTimeout) {
		this.codeTimeout = codeTimeout;
	}

	public Long getContainerTimeout() {
		return containerTimeout;
	}

	public void setContainerTimeout(Long containerTimeout) {
		this.containerTimeout = containerTimeout;
	}

	public String getNetworkMode() {
		return networkMode;
	}

	public void setNetworkMode(String networkMode) {
		this.networkMode = networkMode;
	}

}

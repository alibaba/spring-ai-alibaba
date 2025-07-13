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

public class ContainerProperties {

	public enum ContainerImpl {

		DOCKER, CONTAINERD, KATA;

	}

	ContainerImpl containerImpl = ContainerImpl.DOCKER;

	// 服务Host，为null则使用默认地址
	String host = null;

	String imageName = "python:3-slim";

	String containerNamePrefix = "nl2sql-python-exec-";

	Integer taskQueueSize = 5;

	Integer coreContainerNum = 2;

	Integer tempContainerNum = 2;

	Integer coreThreadSize = 5;

	Integer maxThreadSize = 5;

	// 单位分
	Integer tempContainerAliveTime = 5;

	// 单位秒
	Long keepThreadAliveTime = 60L;

	Integer threadQueueSize = 10;

	/**
	 * Memory size limit (MB)
	 */
	Long limitMemory = 500L;

	/**
	 * Container CPU core limit
	 */
	Long cpuCore = 1L;

	/**
	 * Timeout of python code
	 */
	String codeTimeout = "60s";

	/**
	 * Timeout of Docker (s)
	 */
	Long dockerTimeout = 3000L;

	String networkMode = "bridge";

	public ContainerImpl getContainerImpl() {
		return containerImpl;
	}

	public void setContainerImpl(ContainerImpl containerImpl) {
		this.containerImpl = containerImpl;
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

	public Long getDockerTimeout() {
		return dockerTimeout;
	}

	public void setDockerTimeout(Long dockerTimeout) {
		this.dockerTimeout = dockerTimeout;
	}

	public String getNetworkMode() {
		return networkMode;
	}

	public void setNetworkMode(String networkMode) {
		this.networkMode = networkMode;
	}

}

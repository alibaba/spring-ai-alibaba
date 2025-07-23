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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author vlsmb
 * @since 2025/7/12
 */
@ConfigurationProperties(prefix = ContainerProperties.CONFIG_PREFIX)
public class ContainerProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.nl2sql.container";

	public enum ContainerImpl {

		DOCKER, CONTAINERD, KATA;

	}

	Boolean enabled = true;

	/**
	 * 指定容器池的实现类
	 */
	ContainerImpl containerImpl = ContainerImpl.DOCKER;

	/**
	 * 服务Host，为null则使用默认地址
	 */
	String host = null;

	/**
	 * 镜像名称，可以自定义带有常用第三方依赖的镜像来替换此配置
	 */
	String imageName = "python:3-slim";

	/**
	 * 容器名称前缀
	 */
	String containerNamePrefix = "nl2sql-python-exec-";

	/**
	 * 任务阻塞队列大小
	 */
	Integer taskQueueSize = 5;

	/**
	 * 核心容器的最大数量
	 */
	Integer coreContainerNum = 2;

	/**
	 * 临时容器的最大数量
	 */
	Integer tempContainerNum = 2;

	/**
	 * 线程池的核心线程数量
	 */
	Integer coreThreadSize = 5;

	/**
	 * 线程池的最大线程数量
	 */
	Integer maxThreadSize = 5;

	/**
	 * 临时容器的存活时间，单位分
	 */
	Integer tempContainerAliveTime = 5;

	/**
	 * 线程池的任务存活时间，单位秒
	 */
	Long keepThreadAliveTime = 60L;

	/**
	 * 线程池的任务阻塞队列大小
	 */
	Integer threadQueueSize = 10;

	/**
	 * 容器最大内存，单位MB
	 */
	Long limitMemory = 500L;

	/**
	 * 容器CPU核心数
	 */
	Long cpuCore = 1L;

	/**
	 * Python代码运行上限时间
	 */
	String codeTimeout = "60s";

	/**
	 * 容器运行最大时间
	 */
	Long containerTimeout = 3000L;

	/**
	 * 容器网络模式
	 */
	String networkMode = "bridge";

	public Boolean getEnabled() {
		return enabled;
	}

	public void setEnabled(Boolean enabled) {
		this.enabled = enabled;
	}

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

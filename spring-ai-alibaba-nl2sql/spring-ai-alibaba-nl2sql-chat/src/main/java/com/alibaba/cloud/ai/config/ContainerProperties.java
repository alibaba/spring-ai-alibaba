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

	// 服务Host，为null则使用默认地址
	String host = null;

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

	public Integer getTaskQueueSize() {
		return taskQueueSize;
	}

	public void setTaskQueueSize(Integer taskQueueSize) {
		this.taskQueueSize = taskQueueSize;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
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

}

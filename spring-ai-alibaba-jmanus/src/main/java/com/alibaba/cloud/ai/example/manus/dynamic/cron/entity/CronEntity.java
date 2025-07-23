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
package com.alibaba.cloud.ai.example.manus.dynamic.cron.entity;

import com.alibaba.cloud.ai.example.manus.dynamic.cron.vo.CronConfig;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "cron_task")
public class CronEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String cronName;

	@Column(nullable = false)
	private String cronTime;

	@Column(nullable = false)
	private String planDesc;

	@Column(nullable = false)
	private Integer status;

	@Column(nullable = false)
	private LocalDateTime createTime;

	@Column
	private LocalDateTime lastExecutedTime;

	public CronEntity() {
	}

	public CronEntity(Long id) {
		this.id = id;
	}

	// Getters and Setters
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCronName() {
		return cronName;
	}

	public void setCronName(String cronName) {
		this.cronName = cronName;
	}

	public String getCronTime() {
		return cronTime;
	}

	public void setCronTime(String cronTime) {
		this.cronTime = cronTime;
	}

	public String getPlanDesc() {
		return planDesc;
	}

	public void setPlanDesc(String planDesc) {
		this.planDesc = planDesc;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public Integer getStatus() {
		return status;
	}

	public void setStatus(Integer status) {
		this.status = status;
	}

	public LocalDateTime getLastExecutedTime() {
		return lastExecutedTime;
	}

	public void setLastExecutedTime(LocalDateTime lastExecutedTime) {
		this.lastExecutedTime = lastExecutedTime;
	}

	public CronConfig mapToCronConfig() {
		CronConfig config = new CronConfig();
		config.setId(this.getId());
		config.setCronName(this.getCronName());
		config.setCronTime(this.getCronTime());
		config.setPlanDesc(this.getPlanDesc());
		config.setStatus(this.getStatus());
		config.setCreateTime(this.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
		config.setLastExecutedTime(this.getLastExecutedTime());
		return config;
	}

}

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
package com.alibaba.cloud.ai.example.manus.recorder.entity;

import com.alibaba.cloud.ai.example.manus.recorder.converter.StringAttributeConverter;
import jakarta.persistence.*;

import java.util.Date;

/**
 * Plan execution record class for tracking and recording detailed information about agent
 * execution process.
 */
@Entity
@Table(name = "plan_execution_record")
public class PlanExecutionRecordEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String planId;

	@Column(nullable = false)
	private Date gmtCreate;

	@Column(nullable = false)
	private Date gmtModified;

	@Convert(converter = StringAttributeConverter.class)
	@Column(columnDefinition = "text", length = 400000)
	private PlanExecutionRecord planExecutionRecord;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public Date getGmtCreate() {
		return gmtCreate;
	}

	public void setGmtCreate(Date gmtCreate) {
		this.gmtCreate = gmtCreate;
	}

	public Date getGmtModified() {
		return gmtModified;
	}

	public void setGmtModified(Date gmtModified) {
		this.gmtModified = gmtModified;
	}

	public PlanExecutionRecord getPlanExecutionRecord() {
		return planExecutionRecord;
	}

	public void setPlanExecutionRecord(PlanExecutionRecord planExecutionRecord) {
		this.planExecutionRecord = planExecutionRecord;
	}

}

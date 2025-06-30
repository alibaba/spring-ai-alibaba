package com.alibaba.cloud.ai.example.manus.recorder.entity;

import com.alibaba.cloud.ai.example.manus.recorder.converter.StringAttributeConverter;
import jakarta.persistence.*;

import java.util.Date;

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
	@Column(columnDefinition = "text")
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

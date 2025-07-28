package com.alibaba.cloud.ai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 智能体数据源关联实体类
 *
 * @author Alibaba Cloud AI
 */
public class AgentDatasource {

	private Integer id;

	private Integer agentId;

	private Integer datasourceId;

	private Integer isActive;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createTime;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateTime;

	// 关联的数据源对象（用于联查）
	private Datasource datasource;

	// 构造函数
	public AgentDatasource() {
	}

	public AgentDatasource(Integer agentId, Integer datasourceId) {
		this.agentId = agentId;
		this.datasourceId = datasourceId;
		this.isActive = 1;
	}

	// Getter 和 Setter 方法
	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public Integer getAgentId() {
		return agentId;
	}

	public void setAgentId(Integer agentId) {
		this.agentId = agentId;
	}

	public Integer getDatasourceId() {
		return datasourceId;
	}

	public void setDatasourceId(Integer datasourceId) {
		this.datasourceId = datasourceId;
	}

	public Integer getIsActive() {
		return isActive;
	}

	public void setIsActive(Integer isActive) {
		this.isActive = isActive;
	}

	public LocalDateTime getCreateTime() {
		return createTime;
	}

	public void setCreateTime(LocalDateTime createTime) {
		this.createTime = createTime;
	}

	public LocalDateTime getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(LocalDateTime updateTime) {
		this.updateTime = updateTime;
	}

	public Datasource getDatasource() {
		return datasource;
	}

	public void setDatasource(Datasource datasource) {
		this.datasource = datasource;
	}

	@Override
	public String toString() {
		return "AgentDatasource{" + "id=" + id + ", agentId=" + agentId + ", datasourceId=" + datasourceId
				+ ", isActive=" + isActive + ", createTime=" + createTime + ", updateTime=" + updateTime + '}';
	}

}

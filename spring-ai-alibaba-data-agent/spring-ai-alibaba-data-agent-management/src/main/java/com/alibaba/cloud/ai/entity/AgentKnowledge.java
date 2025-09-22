/**
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.entity;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * Agent Knowledge Entity Class
 */
public class AgentKnowledge {

	private Integer id;

	private Integer agentId;

	private String title;

	private String content;

	private String type; // document, qa, faq

	private String category;

	private String tags;

	private String status; // active, inactive

	private String sourceUrl;

	private String filePath;

	private Long fileSize;

	private String fileType;

	private String embeddingStatus; // pending, processing, completed, failed

	private Long creatorId;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime createTime;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime updateTime;

	// Default constructor
	public AgentKnowledge() {
	}

	// All-args constructor
	public AgentKnowledge(Integer id, Integer agentId, String title, String content, String type, String category,
			String tags, String status, String sourceUrl, String filePath, Long fileSize, String fileType,
			String embeddingStatus, Long creatorId, LocalDateTime createTime, LocalDateTime updateTime) {
		this.id = id;
		this.agentId = agentId;
		this.title = title;
		this.content = content;
		this.type = type;
		this.category = category;
		this.tags = tags;
		this.status = status;
		this.sourceUrl = sourceUrl;
		this.filePath = filePath;
		this.fileSize = fileSize;
		this.fileType = fileType;
		this.embeddingStatus = embeddingStatus;
		this.creatorId = creatorId;
		this.createTime = createTime;
		this.updateTime = updateTime;
	}

	// Getters and Setters
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

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getTags() {
		return tags;
	}

	public void setTags(String tags) {
		this.tags = tags;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public Long getFileSize() {
		return fileSize;
	}

	public void setFileSize(Long fileSize) {
		this.fileSize = fileSize;
	}

	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	public String getEmbeddingStatus() {
		return embeddingStatus;
	}

	public void setEmbeddingStatus(String embeddingStatus) {
		this.embeddingStatus = embeddingStatus;
	}

	public Long getCreatorId() {
		return creatorId;
	}

	public void setCreatorId(Long creatorId) {
		this.creatorId = creatorId;
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

	@Override
	public String toString() {
		return "AgentKnowledge{" + "id=" + id + ", agentId=" + agentId + ", title='" + title + '\'' + ", content='"
				+ content + '\'' + ", type='" + type + '\'' + ", category='" + category + '\'' + ", tags='" + tags
				+ '\'' + ", status='" + status + '\'' + ", sourceUrl='" + sourceUrl + '\'' + ", filePath='" + filePath
				+ '\'' + ", fileSize=" + fileSize + ", fileType='" + fileType + '\'' + ", embeddingStatus='"
				+ embeddingStatus + '\'' + ", creatorId=" + creatorId + ", createTime=" + createTime + ", updateTime="
				+ updateTime + '}';
	}

}

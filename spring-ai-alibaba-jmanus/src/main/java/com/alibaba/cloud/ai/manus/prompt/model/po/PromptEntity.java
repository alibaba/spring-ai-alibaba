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
package com.alibaba.cloud.ai.manus.prompt.model.po;

import jakarta.persistence.*;

@Entity
@Table(name = "prompt", uniqueConstraints = { @UniqueConstraint(columnNames = { "namespace", "prompt_name" }) })
public class PromptEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "prompt_name", nullable = false)
	private String promptName;

	@Column(nullable = true)
	private String namespace;

	@Column(name = "message_type", nullable = false)
	private String messageType;

	@Column(name = "type", nullable = false)
	private String type;

	@Column(name = "built_in", nullable = false)
	private Boolean builtIn;

	@Column(name = "prompt_description", nullable = false, length = 1024)
	private String promptDescription;

	@Column(name = "prompt_content", columnDefinition = "TEXT", nullable = false)
	private String promptContent;

	/**
	 * Default constructor required by Hibernate/JPA
	 */
	public PromptEntity() {
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getPromptName() {
		return promptName;
	}

	public void setPromptName(String promptName) {
		this.promptName = promptName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getMessageType() {
		return messageType;
	}

	public void setMessageType(String messageType) {
		this.messageType = messageType;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getPromptContent() {
		return promptContent;
	}

	public void setPromptContent(String promptContent) {
		this.promptContent = promptContent;
	}

	public Boolean getBuiltIn() {
		return builtIn;
	}

	public void setBuiltIn(Boolean builtIn) {
		this.builtIn = builtIn;
	}

	public String getPromptDescription() {
		return promptDescription;
	}

	public void setPromptDescription(String promptDescription) {
		this.promptDescription = promptDescription;
	}

}

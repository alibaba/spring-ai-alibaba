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
package com.alibaba.cloud.ai.manus.prompt.model.vo;

import org.springframework.ai.chat.messages.MessageType;

import com.alibaba.cloud.ai.manus.prompt.model.enums.PromptType;

public class PromptVO {

	private Long id;

	private String promptName;

	private String namespace;

	private String messageType;

	private String type;

	private String promptContent;

	private Boolean builtIn;

	private String promptDescription;

	public Boolean invalid() {
		return promptName == null || messageType == null || type == null || promptContent == null
				|| promptDescription == null || builtIn == null || !isValidEnumValue(type, PromptType.class)
				|| !isValidEnumValue(messageType, MessageType.class);
	}

	private static <E extends Enum<E>> boolean isValidEnumValue(String value, Class<E> enumClass) {
		if (value == null) {
			return false;
		}
		try {
			Enum.valueOf(enumClass, value);
			return true;
		}
		catch (IllegalArgumentException e) {
			return false;
		}
	}

	public PromptVO() {
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

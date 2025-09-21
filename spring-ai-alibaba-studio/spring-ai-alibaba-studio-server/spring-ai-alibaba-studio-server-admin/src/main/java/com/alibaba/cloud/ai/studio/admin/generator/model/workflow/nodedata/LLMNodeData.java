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

package com.alibaba.cloud.ai.studio.admin.generator.model.workflow.nodedata;

import com.alibaba.cloud.ai.studio.admin.generator.model.Variable;
import com.alibaba.cloud.ai.studio.admin.generator.model.VariableType;
import com.alibaba.cloud.ai.studio.admin.generator.model.workflow.NodeData;
import com.alibaba.cloud.ai.studio.admin.generator.service.dsl.DSLDialectType;
import com.alibaba.cloud.ai.studio.admin.generator.utils.ObjectToCodeUtil;
import org.springframework.ai.chat.messages.MessageType;

import java.util.List;
import java.util.Map;

public class LLMNodeData extends NodeData {

	public static List<Variable> getDefaultOutputSchemas(DSLDialectType dialectType) {
		return switch (dialectType) {
			case DIFY -> List.of(new Variable("text", VariableType.STRING));
			case STUDIO -> List.of(new Variable("output", VariableType.STRING),
					new Variable("reasoning_content", VariableType.STRING));
			default -> List.of();
		};
	}

	private String chatModeName;

	private Map<String, Object> modeParams;

	private List<MessageTemplate> messageTemplates;

	private String memoryKey;

	private Integer maxRetryCount;

	private Integer retryIntervalMs;

	private String defaultOutput;

	private String errorNextNode;

	private String outputKeyPrefix;

	public String getChatModeName() {
		return chatModeName;
	}

	public void setChatModeName(String chatModeName) {
		this.chatModeName = chatModeName;
	}

	public Map<String, Object> getModeParams() {
		return modeParams;
	}

	public void setModeParams(Map<String, Object> modeParams) {
		this.modeParams = modeParams;
	}

	public List<MessageTemplate> getMessageTemplates() {
		return messageTemplates;
	}

	public void setMessageTemplates(List<MessageTemplate> messageTemplates) {
		this.messageTemplates = messageTemplates;
	}

	public String getMemoryKey() {
		return memoryKey;
	}

	public void setMemoryKey(String memoryKey) {
		this.memoryKey = memoryKey;
	}

	public Integer getMaxRetryCount() {
		return maxRetryCount;
	}

	public void setMaxRetryCount(Integer maxRetryCount) {
		this.maxRetryCount = maxRetryCount;
	}

	public Integer getRetryIntervalMs() {
		return retryIntervalMs;
	}

	public void setRetryIntervalMs(Integer retryIntervalMs) {
		this.retryIntervalMs = retryIntervalMs;
	}

	public String getDefaultOutput() {
		return defaultOutput;
	}

	public void setDefaultOutput(String defaultOutput) {
		this.defaultOutput = defaultOutput;
	}

	public String getErrorNextNode() {
		return errorNextNode;
	}

	public void setErrorNextNode(String errorNextNode) {
		this.errorNextNode = errorNextNode;
	}

	public String getOutputKeyPrefix() {
		return outputKeyPrefix;
	}

	public void setOutputKeyPrefix(String outputKeyPrefix) {
		this.outputKeyPrefix = outputKeyPrefix;
	}

	public record MessageTemplate(String template, List<String> keys, MessageType type) {

		@Override
		public String toString() {
			return String.format("new MessageTemplate(%s, %s, MessageType.%s)",
					ObjectToCodeUtil.toCode(this.template()), ObjectToCodeUtil.toCode(this.keys()),
					this.type().getValue().toUpperCase());
		}
	}

}

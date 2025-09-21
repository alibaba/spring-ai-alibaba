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
package com.alibaba.cloud.ai.autoconfigure.arms;

import com.alibaba.cloud.ai.observation.model.semconv.MessageMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Lumian
 */
@ConfigurationProperties(ArmsCommonProperties.CONFIG_PREFIX)
public class ArmsCommonProperties {

	/**
	 * Spring AI Alibaba ARMS extension configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.alibaba.arms";

	/**
	 * Enable Arms instrumentations and conventions.
	 */
	private boolean enabled = false;

	private ModelProperties model = new ModelProperties();

	private ToolProperties tool = new ToolProperties();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public ModelProperties getModel() {
		return model;
	}

	public void setModel(ModelProperties model) {
		this.model = model;
	}

	public ToolProperties getTool() {
		return tool;
	}

	public void setTool(ToolProperties tool) {
		this.tool = tool;
	}

	public static class ModelProperties {

		/**
		 * Enable Arms instrumentations and conventions.
		 */
		private boolean captureInput = false;

		/**
		 * Enable Arms instrumentations and conventions.
		 */
		private boolean captureOutput = false;

		/**
		 * Arms export type enumeration.
		 */
		private MessageMode messageMode = MessageMode.OPEN_TELEMETRY;

		public boolean isCaptureInput() {
			return captureInput;
		}

		public void setCaptureInput(boolean captureInput) {
			this.captureInput = captureInput;
		}

		public boolean isCaptureOutput() {
			return captureOutput;
		}

		public void setCaptureOutput(boolean captureOutput) {
			this.captureOutput = captureOutput;
		}

		public MessageMode getMessageMode() {
			return messageMode;
		}

		public void setMessageMode(MessageMode messageMode) {
			this.messageMode = messageMode;
		}

	}

	public static class ToolProperties {

		private boolean enabled = true;

		public boolean isEnabled() {
			return enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

	}

}

/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.messagechannel.autoconfigure;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration root for the message-channel starter.
 *
 * <p>Each entry under {@code channels} declares one bound IM platform; the map key
 * becomes both the URL path variable ({@code /channel/<key>/callback}) and the
 * {@link com.alibaba.cloud.ai.messagechannel.adapter.MessageChannelAdapter#name()
 * adapter name}.</p>
 */
@ConfigurationProperties(prefix = MessageChannelProperties.CONFIG_PREFIX)
public class MessageChannelProperties {

	public static final String CONFIG_PREFIX = "spring.ai.alibaba.message-channel";

	private boolean enabled = true;

	private String basePath = "/channel";

	private Map<String, ChannelConfig> channels = new LinkedHashMap<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBasePath() {
		return basePath;
	}

	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	public Map<String, ChannelConfig> getChannels() {
		return channels;
	}

	public void setChannels(Map<String, ChannelConfig> channels) {
		this.channels = channels;
	}

	/** Per-channel configuration. */
	public static class ChannelConfig {

		/** {@code dingtalk}, {@code feishu}, {@code wechat-work}, ... */
		private String type;

		/** Bean name of the {@code Agent} this channel routes inbound messages to. */
		private String bindAgent;

		/** Type-specific options (app-key, app-secret, webhook-url, ...). */
		private Map<String, String> options = new LinkedHashMap<>();

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}

		public String getBindAgent() {
			return bindAgent;
		}

		public void setBindAgent(String bindAgent) {
			this.bindAgent = bindAgent;
		}

		public Map<String, String> getOptions() {
			return options;
		}

		public void setOptions(Map<String, String> options) {
			this.options = options;
		}

	}

}

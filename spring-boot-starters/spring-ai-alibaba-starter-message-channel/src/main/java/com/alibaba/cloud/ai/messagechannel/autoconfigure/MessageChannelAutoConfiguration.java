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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.alibaba.cloud.ai.messagechannel.adapter.ChannelAdapterRegistry;
import com.alibaba.cloud.ai.messagechannel.adapter.MessageChannelAdapter;
import com.alibaba.cloud.ai.messagechannel.adapter.dingtalk.DingTalkChannelAdapter;
import com.alibaba.cloud.ai.messagechannel.dispatcher.AgentBindingRegistry;
import com.alibaba.cloud.ai.messagechannel.dispatcher.MessageChannelDispatcher;
import com.alibaba.cloud.ai.messagechannel.publisher.MessagePublisher;
import com.alibaba.cloud.ai.messagechannel.web.MessageChannelController;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration that wires the registry/dispatcher/controller/publisher and
 * builds one {@link MessageChannelAdapter} per entry in
 * {@link MessageChannelProperties#getChannels()} according to its declared type.
 */
@AutoConfiguration(after = JacksonAutoConfiguration.class)
@EnableConfigurationProperties(MessageChannelProperties.class)
@ConditionalOnProperty(prefix = MessageChannelProperties.CONFIG_PREFIX, name = "enabled",
		havingValue = "true", matchIfMissing = true)
public class MessageChannelAutoConfiguration {

	@Bean
	public RestClient messageChannelRestClient(ObjectProvider<RestClient.Builder> builderProvider) {
		RestClient.Builder builder = builderProvider.getIfAvailable(RestClient::builder);
		return builder.build();
	}

	@Bean
	public ChannelAdapterRegistry channelAdapterRegistry(MessageChannelProperties properties,
			List<MessageChannelAdapter> userAdapters,
			ObjectMapper objectMapper,
			RestClient messageChannelRestClient) {
		List<MessageChannelAdapter> all = new ArrayList<>(userAdapters);
		for (Map.Entry<String, MessageChannelProperties.ChannelConfig> e : properties.getChannels().entrySet()) {
			String name = e.getKey();
			if (containsByName(userAdapters, name)) {
				continue;
			}
			MessageChannelAdapter adapter = buildBuiltInAdapter(name, e.getValue(), objectMapper,
					messageChannelRestClient);
			if (adapter != null) {
				all.add(adapter);
			}
		}
		return new ChannelAdapterRegistry(all);
	}

	@Bean
	public AgentBindingRegistry agentBindingRegistry(BeanFactory beanFactory, MessageChannelProperties properties) {
		Map<String, String> map = new LinkedHashMap<>();
		properties.getChannels().forEach((k, v) -> {
			if (v.getBindAgent() != null && !v.getBindAgent().isBlank()) {
				map.put(k, v.getBindAgent());
			}
		});
		return new AgentBindingRegistry(beanFactory, map);
	}

	@Bean
	public MessageChannelDispatcher messageChannelDispatcher(AgentBindingRegistry bindings) {
		return new MessageChannelDispatcher(bindings);
	}

	@Bean
	public MessagePublisher messagePublisher(ChannelAdapterRegistry registry) {
		return new MessagePublisher(registry);
	}

	@Bean
	public MessageChannelController messageChannelController(ChannelAdapterRegistry registry,
			MessageChannelDispatcher dispatcher) {
		return new MessageChannelController(registry, dispatcher);
	}

	private MessageChannelAdapter buildBuiltInAdapter(String name, MessageChannelProperties.ChannelConfig config,
			ObjectMapper objectMapper, RestClient restClient) {
		String type = config.getType();
		if (type == null) {
			type = name;
		}
		return switch (type.toLowerCase()) {
			case "dingtalk" -> new DingTalkChannelAdapter(
					name,
					requireOption(config, "app-secret", name),
					config.getOptions().get("webhook-url"),
					objectMapper,
					restClient);
			default -> null;
		};
	}

	private static String requireOption(MessageChannelProperties.ChannelConfig config, String key, String channel) {
		String v = config.getOptions().get(key);
		if (v == null || v.isBlank()) {
			throw new IllegalStateException("Channel '" + channel + "' missing required option '" + key + "'");
		}
		return v;
	}

	private static boolean containsByName(List<MessageChannelAdapter> adapters, String name) {
		for (MessageChannelAdapter a : adapters) {
			if (name.equals(a.name())) {
				return true;
			}
		}
		return false;
	}

}

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

import com.alibaba.cloud.ai.messagechannel.adapter.ChannelAdapterRegistry;
import com.alibaba.cloud.ai.messagechannel.adapter.MessageChannelAdapter;
import com.alibaba.cloud.ai.messagechannel.adapter.dingtalk.DingTalkChannelAdapter;
import com.alibaba.cloud.ai.messagechannel.dispatcher.MessageChannelDispatcher;
import com.alibaba.cloud.ai.messagechannel.publisher.MessagePublisher;
import com.alibaba.cloud.ai.messagechannel.web.MessageChannelController;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class MessageChannelAutoConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class,
					MessageChannelAutoConfiguration.class));

	@Test
	void disabledByProperty_skipsAllBeans() {
		contextRunner.withPropertyValues("spring.ai.alibaba.message-channel.enabled=false")
				.run(ctx -> {
					assertThat(ctx).doesNotHaveBean(ChannelAdapterRegistry.class);
					assertThat(ctx).doesNotHaveBean(MessageChannelDispatcher.class);
					assertThat(ctx).doesNotHaveBean(MessagePublisher.class);
					assertThat(ctx).doesNotHaveBean(MessageChannelController.class);
				});
	}

	@Test
	void enabledByDefault_wiresAllInfrastructureBeans() {
		contextRunner.run(ctx -> {
			assertThat(ctx).hasSingleBean(ChannelAdapterRegistry.class);
			assertThat(ctx).hasSingleBean(MessageChannelDispatcher.class);
			assertThat(ctx).hasSingleBean(MessagePublisher.class);
			assertThat(ctx).hasSingleBean(MessageChannelController.class);
			assertThat(ctx).hasSingleBean(MessageChannelProperties.class);
		});
	}

	@Test
	void dingtalkChannelConfig_buildsBuiltInAdapter() {
		contextRunner.withPropertyValues(
				"spring.ai.alibaba.message-channel.channels.dingtalk.type=dingtalk",
				"spring.ai.alibaba.message-channel.channels.dingtalk.bind-agent=customerAgent",
				"spring.ai.alibaba.message-channel.channels.dingtalk.options.app-secret=topsecret",
				"spring.ai.alibaba.message-channel.channels.dingtalk.options.webhook-url=https://example/robot")
				.run(ctx -> {
					assertThat(ctx).hasBean("channelAdapterRegistry");
					ChannelAdapterRegistry registry = ctx.getBean(ChannelAdapterRegistry.class);
					assertThat(registry.contains("dingtalk")).isTrue();
					MessageChannelAdapter adapter = registry.require("dingtalk");
					assertThat(adapter).isInstanceOf(DingTalkChannelAdapter.class);
				});
	}

	@Test
	void dingtalkChannelMissingSecret_failsContextStartup() {
		contextRunner.withPropertyValues(
				"spring.ai.alibaba.message-channel.channels.dingtalk.type=dingtalk",
				"spring.ai.alibaba.message-channel.channels.dingtalk.bind-agent=customerAgent")
				.run(ctx -> {
					assertThat(ctx).hasFailed();
					assertThat(ctx.getStartupFailure()).rootCause()
							.hasMessageContaining("missing required option 'app-secret'");
				});
	}

	@Test
	void unknownChannelType_isIgnored() {
		contextRunner.withPropertyValues(
				"spring.ai.alibaba.message-channel.channels.exotic.type=unknown-platform",
				"spring.ai.alibaba.message-channel.channels.exotic.bind-agent=anAgent")
				.run(ctx -> {
					ChannelAdapterRegistry registry = ctx.getBean(ChannelAdapterRegistry.class);
					assertThat(registry.contains("exotic")).isFalse();
				});
	}

}

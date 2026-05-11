/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.multimodal.image;

import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.retry.RetryUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import static com.alibaba.cloud.ai.dashscope.common.DashScopeApiConstants.DEFAULT_BASE_URL;

/**
 * Configuration for image-related agents and components.
 *
 * <p>ImageModel: Prefer {@code DashScopeImageAutoConfiguration} when it takes effect.
 * This manual bean is a fallback when auto-config does not apply (e.g. due to property
 * resolution, classpath, or condition evaluation order).
 */
@Configuration
public class ImageConfig {

	private static final String VISION_AGENT_INSTRUCTION = """
			You are a helpful vision assistant with the ability to understand and analyze images.
			When the user provides an image, describe what you see in detail.
			You can answer questions about the image content, identify objects, text, and scenes.
			Be concise but thorough in your descriptions.
			""";

	@Bean("visionAgent")
	public ReactAgent visionAgent(ChatModel chatModel) {
		return ReactAgent.builder()
				.name("vision_assistant")
				.instruction(VISION_AGENT_INSTRUCTION)
				.model(chatModel)
				.build();
	}
}

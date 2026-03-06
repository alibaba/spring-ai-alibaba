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
package com.alibaba.cloud.ai.examples.multimodal.creative;

import com.alibaba.cloud.ai.examples.multimodal.image.GenerateImageTool;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for creative agent (image generation via tools).
 * TTS uses DashScopeAudioSpeechModel directly via /api/audio/tts.
 *
 * <p>Requires {@link GenerateImageTool} (and thus {@link org.springframework.ai.image.ImageModel}).
 * Spring will create dependencies in order automatically.
 */
@Configuration
public class CreativeConfig {

	private static final String CREATIVE_AGENT_INSTRUCTION = """
			You are a creative assistant that can generate images.
			Use generate_image when the user wants to create, draw, or visualize something.
			Always confirm what you generated and provide the output (URL) in your response.
			""";

	@Bean("creativeAgent")
	public ReactAgent creativeAgent(ChatModel chatModel, GenerateImageTool generateImageTool) {
		return ReactAgent.builder()
				.name("creative_assistant")
				.instruction(CREATIVE_AGENT_INSTRUCTION)
				.model(chatModel)
				.methodTools(generateImageTool)
				.build();
	}
}

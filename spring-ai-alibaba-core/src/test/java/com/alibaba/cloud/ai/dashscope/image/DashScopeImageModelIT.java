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
package com.alibaba.cloud.ai.dashscope.image;

import com.alibaba.cloud.ai.dashscope.DashscopeAiTestConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import org.springframework.ai.image.Image;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.image.ImageResponseMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = DashscopeAiTestConfiguration.class)
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_API_KEY", matches = ".+")
@EnabledIfEnvironmentVariable(named = "DASHSCOPE_HTTP_BASE_URL", matches = ".+")
public class DashScopeImageModelIT {

	@Autowired
	protected ImageModel imageModel;

	@Test
	void imageAsUrlTest() {
		var options = ImageOptionsBuilder.builder().withHeight(1024).withWidth(1024).build();

		var instructions = """
				A light cream colored mini golden doodle with a sign that contains the message "I'm on my way to BARCADE!".""";

		ImagePrompt imagePrompt = new ImagePrompt(instructions, options);

		ImageResponse imageResponse = imageModel.call(imagePrompt);

		assertThat(imageResponse.getResults()).hasSize(1);

		ImageResponseMetadata imageResponseMetadata = imageResponse.getMetadata();
		assertThat(imageResponseMetadata.getCreated()).isPositive();

		var generation = imageResponse.getResult();
		Image image = generation.getOutput();
		assertThat(image.getUrl()).isNotEmpty();
		assertThat(image.getB64Json()).isNull();
	}

}

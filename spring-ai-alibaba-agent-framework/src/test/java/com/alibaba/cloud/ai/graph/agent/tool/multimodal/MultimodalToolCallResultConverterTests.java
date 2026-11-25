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
package com.alibaba.cloud.ai.graph.agent.tool.multimodal;

import org.junit.jupiter.api.Test;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MultimodalToolCallResultConverterTests {

	private static final String URL = "https://example.invalid/image.png";

	private static final String BASE64 = "aW1hZ2U=";

	@Test
	void selectsTheExistingReferenceOrInlineDataForDualFormatMedia() {
		Media media = ToolMultimodalResult.mediaFromUrlAndBase64(URL, BASE64, MimeTypeUtils.IMAGE_PNG);
		ToolMultimodalResult result = ToolMultimodalResult.of("Generated image", media);

		assertThat(media.getData()).isEqualTo(URL);
		assertThat(new MultimodalToolCallResultConverter().convert(result, ToolMultimodalResult.class))
			.contains("\"url\":\"" + URL + "\"")
			.doesNotContain("\"data\":");
		assertThat(new MultimodalBase64ToolCallResultConverter().convert(result, ToolMultimodalResult.class))
			.contains("\"data\":\"data:image/png;base64," + BASE64 + "\"")
			.doesNotContain("\"url\":");
	}

	@Test
	void inlineMediaKeepsItsBase64Representation() {
		Media media = ToolMultimodalResult.mediaFromBase64(BASE64, MimeTypeUtils.IMAGE_PNG);
		ToolMultimodalResult result = ToolMultimodalResult.of("Generated image", media);

		assertThat(media.getData()).isEqualTo(BASE64);
		assertThat(new MultimodalBase64ToolCallResultConverter().convert(result, ToolMultimodalResult.class))
			.contains("\"data\":\"data:image/png;base64," + BASE64 + "\"");
	}

}

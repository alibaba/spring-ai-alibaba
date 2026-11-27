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
import org.springframework.util.MimeTypeUtils;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class MultimodalToolCallResultConverterTest {

	@Test
	void preservesEachProvidedRepresentation() {
		String url = "https://example.invalid/image.png";
		var media = ToolMultimodalResult.mediaFromUrlAndBase64(url, "AQID", MimeTypeUtils.IMAGE_PNG);
		var result = ToolMultimodalResult.of("Image generated", media);

		assertThat(media.getData()).isEqualTo(url);
		var urlOutput = mediaOutput(OutputFormat.url, result);
		assertThat(urlOutput.path("url").asString()).isEqualTo(url);
		assertThat(urlOutput.has("data")).isFalse();

		var inlineOutput = mediaOutput(OutputFormat.base64, result);
		assertThat(inlineOutput.path("data").asString()).isEqualTo("data:image/png;base64,AQID");
		assertThat(inlineOutput.has("url")).isFalse();
	}

	@Test
	void exposesBase64OnlyMediaAsADataUri() {
		var media = ToolMultimodalResult.mediaFromBase64("AQID", MimeTypeUtils.IMAGE_PNG);
		var result = ToolMultimodalResult.of("Image generated", media);

		assertThat(media.getData()).isEqualTo("data:image/png;base64,AQID");
		assertThat(mediaOutput(OutputFormat.base64, result).path("data").asString())
			.isEqualTo("data:image/png;base64,AQID");
	}

	private JsonNode mediaOutput(OutputFormat format, ToolMultimodalResult result) {
		String output = new MultimodalToolCallResultConverter(format).convert(result, ToolMultimodalResult.class);
		return JsonMapper.shared().readTree(output).at("/media/0");
	}

}

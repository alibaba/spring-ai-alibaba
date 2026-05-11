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

import com.alibaba.cloud.ai.graph.agent.tool.multimodal.MultimodalToolCallResultConverter;
import com.alibaba.cloud.ai.graph.agent.tool.multimodal.ToolMultimodalResult;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/**
 * Tool that wraps {@link ImageModel} for ReactAgent to generate images from text prompts.
 *
 * <p>Returns {@link ToolMultimodalResult} with image URL(s). Output format (url vs base64)
 * is handled by {@link MultimodalToolCallResultConverter} configuration.
 *
 * <p>Reference: {@code multiagents/dashscope-image}, Strands {@code generate_image} tool.
 */
@Component
public class GenerateImageTool {

	private final ImageModel imageModel;

	public GenerateImageTool(ImageModel imageModel) {
		this.imageModel = imageModel;
	}

	@Tool(name = "generate_image", description = """
			Generate an image from a text prompt. Use this when the user wants to create, draw, or
			generate an image. Returns the image URL. Prompt should be descriptive (subject, style, mood).
			""", resultConverter = MultimodalToolCallResultConverter.class)
	public ToolMultimodalResult generateImage(
			@ToolParam(description = "Detailed text prompt describing the image to generate (e.g., 'a cute dog in a garden, watercolor style')") String prompt,
			@ToolParam(description = "Number of images to generate (default 1)", required = false) Integer n,
			@ToolParam(description = "Image size, e.g. 1024x1024, 1280x720 (default 1024x1024)", required = false) String size) {

		int count = (n != null && n > 0) ? Math.min(n, 4) : 1;
		var builder = ImageOptionsBuilder.builder().N(count);
		if (size != null && !size.isBlank()) {
			try {
				String[] parts = size.split("[x*]");
				if (parts.length == 2) {
					builder.width(Integer.parseInt(parts[0].trim()))
							.height(Integer.parseInt(parts[1].trim()));
				}
			}
			catch (NumberFormatException ignored) {
				// fallback to default
			}
		}
		var options = builder.build();

		ImageResponse response = imageModel.call(new ImagePrompt(prompt, options));
		var results = response.getResults();
		if (results == null || results.isEmpty()) {
			return ToolMultimodalResult.of("Image generation failed: no result returned.");
		}
		ToolMultimodalResult.Builder resultBuilder = ToolMultimodalResult.builder()
				.text(results.size() == 1 ? "Image generated successfully." : "Generated " + results.size() + " images.");
		for (var r : results) {
			var img = r.getOutput();
			String url = img.getUrl();
			String b64 = img.getB64Json();
			if (url != null && !url.isBlank() || b64 != null && !b64.isBlank()) {
				resultBuilder.media(ToolMultimodalResult.mediaFromUrlAndBase64(url, b64, MimeTypeUtils.IMAGE_PNG));
			}
		}
		return resultBuilder.build();
	}
}

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

package com.alibaba.cloud.ai.parser.multi;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.alibaba.cloud.ai.document.DocumentParser;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversation;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationParam;
import com.alibaba.dashscope.aigc.multimodalconversation.MultiModalConversationResult;
import com.alibaba.dashscope.common.MultiModalMessage;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.exception.UploadFileException;
import org.springframework.ai.document.Document;

/**
 * @author HeYQ
 * @since 2025-02-06 23:38
 */

public class ImageDashScopeParser implements DocumentParser {

	private final String model;

	private final String maxPixels;

	private final String minPixels;

	private final String apiKey;

	public ImageDashScopeParser(String apiKey) {
		this(apiKey, "qwen-vl-ocr", "1003520", "3136");
	}

	public ImageDashScopeParser(String apiKey, String model) {
		this(apiKey, model, "1003520", "3136");
	}

	public ImageDashScopeParser(String apiKey, String model, String maxPixels, String minPixels) {
		this.apiKey = apiKey;
		this.model = model;
		this.maxPixels = maxPixels;
		this.minPixels = minPixels;
	}

	public List<Document> parse(String path) {
		try {
			MultiModalConversation conversation = new MultiModalConversation();
			String filePath = "";
			if (path.startsWith("http")) {
				filePath = path;
			}
			else {
				String os = System.getProperty("os.name").toLowerCase();

				if (os.contains("win")) {
					filePath = "file:///" + path;
				}
				else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
					filePath = "file://" + path;
				}
			}
			Map<String, Object> map = new HashMap<>();
			map.put("image", filePath);
			map.put("max_pixels", maxPixels);
			map.put("min_pixels", minPixels);
			MultiModalMessage userMessage = MultiModalMessage.builder()
				.role(Role.USER.getValue())
				.content(Arrays.asList(map, Collections.singletonMap("text", "Read all the text in the image.")))
				.build();
			MultiModalConversationParam param = MultiModalConversationParam.builder()
				.apiKey(apiKey)
				.model(model)
				.message(userMessage)
				.build();
			MultiModalConversationResult result = conversation.call(param);
			return List.of(new Document(
					result.getOutput().getChoices().get(0).getMessage().getContent().get(0).get("text").toString()));

		}
		catch (ApiException | NoApiKeyException | UploadFileException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public List<Document> parse(InputStream inputStream) {
		return List.of();
	}

}

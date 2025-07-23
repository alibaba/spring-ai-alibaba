/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.toolcalling.ollamasearchmodel;

import com.alibaba.cloud.ai.toolcalling.common.RestClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.function.Function;

/**
 * @author dahua
 * @since 2025/07/14
 */
public class OllamaSearchModelService
		implements Function<OllamaSearchModelService.Request, OllamaSearchModelService.Response> {

	private final RestClientTool restClientTool;

	public OllamaSearchModelService(RestClientTool restClientTool) {
		this.restClientTool = restClientTool;
	}

	@Override
	public Response apply(Request request) {
		String result;
		try {
			String htmlContent = restClientTool.get(String.format("/library/%s", request.modelName));
			Document doc = Jsoup.parse(htmlContent);
			Elements elementsWithAlt = doc.select("img[alt]");
			elementsWithAlt.remove();
			doc.outputSettings(new Document.OutputSettings().prettyPrint(true).outline(false).charset("UTF-8"));
			result = doc.text();
		}
		catch (Exception e) {
			result = "This model is not available on the ollama!";
		}
		return new Response(result);
	}

	@JsonClassDescription("get response from ollama search api")
	public record Request(@JsonPropertyDescription("Search model name") String modelName) {
	}

	@JsonClassDescription("ollama search api result")
	public record Response(@JsonPropertyDescription("Receive model detail") String information) {
	}

}

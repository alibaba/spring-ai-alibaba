/*
 * Copyright 2025 the original author or authors.
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
package com.alibaba.cloud.ai.example.manus.tool.searchAPI;

import com.alibaba.cloud.ai.example.manus.tool.ToolCallBiFunctionDef;
import com.alibaba.cloud.ai.example.manus.tool.code.ToolExecuteResult;
import com.alibaba.cloud.ai.example.manus.tool.searchAPI.serpapi.SerpApiProperties;
import com.alibaba.cloud.ai.example.manus.tool.searchAPI.serpapi.SerpApiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.openai.api.OpenAiApi;

public class GoogleSearch implements ToolCallBiFunctionDef<GoogleSearch.GoogleSearchInput> {

	private static final Logger log = LoggerFactory.getLogger(GoogleSearch.class);

	private SerpApiService service;

	private static String PARAMETERS = """
			{
			    "type": "object",
			    "properties": {
			        "query": {
			            "type": "string",
			            "description": "(required) The search query to submit to Google."
			        },
			        "num_results": {
			            "type": "integer",
			            "description": "(optional) The number of search results to return. Default is 10.",
			            "default": 10
			        }
			    },
			    "required": ["query"]
			}
			""";

	private static final String name = "google_search";

	private static final String description = """
			Perform a Google search and return a list of relevant links.
			Use this tool when you need to find information on the web, get up-to-date data, or research specific topics.
			The tool returns a list of URLs that match the search query.
			""";

	public static OpenAiApi.FunctionTool getToolDefinition() {
		OpenAiApi.FunctionTool.Function function = new OpenAiApi.FunctionTool.Function(description, name, PARAMETERS);
		OpenAiApi.FunctionTool functionTool = new OpenAiApi.FunctionTool(function);
		return functionTool;
	}

	private static final String SERP_API_KEY = System.getenv("SERP_API_KEY");

	private String lastQuery = "";

	private String lastSearchResults = "";

	private Integer lastNumResults = 0;

	public GoogleSearch() {
		service = new SerpApiService(new SerpApiProperties(SERP_API_KEY, "google"));
	}

	public ToolExecuteResult run(String toolInput) {
		log.info("GoogleSearch toolInput:{}", toolInput);

		// Add exception handling for JSON deserialization
		try {
			Map<String, Object> toolInputMap = new ObjectMapper().readValue(toolInput,
					new TypeReference<Map<String, Object>>() {
					});
			String query = (String) toolInputMap.get("query");
			this.lastQuery = query;

			Integer numResults = 2;
			if (toolInputMap.get("num_results") != null) {
				numResults = (Integer) toolInputMap.get("num_results");
			}
			this.lastNumResults = numResults;

			SerpApiService.Request request = new SerpApiService.Request(query);
			Map<String, Object> response = service.apply(request);

			if (response.containsKey("answer_box") && response.get("answer_box") instanceof List) {
				response.put("answer_box", ((List<Map<String, Object>>) response.get("answer_box")).get(0));
			}

			String toret = "";
			if (response.containsKey("answer_box")
					&& ((Map<String, Object>) response.get("answer_box")).containsKey("answer")) {
				toret = ((Map<String, Object>) response.get("answer_box")).get("answer").toString();
			}
			else if (response.containsKey("answer_box")
					&& ((Map<String, Object>) response.get("answer_box")).containsKey("snippet")) {
				toret = ((Map<String, Object>) response.get("answer_box")).get("snippet").toString();
			}
			else if (response.containsKey("answer_box")
					&& ((Map<String, Object>) response.get("answer_box")).containsKey("snippet_highlighted_words")) {
				toret = ((List<String>) ((Map<String, Object>) response.get("answer_box"))
					.get("snippet_highlighted_words")).get(0);
			}
			else if (response.containsKey("sports_results")
					&& ((Map<String, Object>) response.get("sports_results")).containsKey("game_spotlight")) {
				toret = ((Map<String, Object>) response.get("sports_results")).get("game_spotlight").toString();
			}
			else if (response.containsKey("shopping_results")
					&& ((List<Map<String, Object>>) response.get("shopping_results")).get(0).containsKey("title")) {
				List<Map<String, Object>> shoppingResults = (List<Map<String, Object>>) response
					.get("shopping_results");
				List<Map<String, Object>> subList = shoppingResults.subList(0, 3);
				toret = subList.toString();
			}
			else if (response.containsKey("knowledge_graph")
					&& ((Map<String, Object>) response.get("knowledge_graph")).containsKey("description")) {
				toret = ((Map<String, Object>) response.get("knowledge_graph")).get("description").toString();
			}
			else if ((((List<Map<String, Object>>) response.get("organic_results")).get(0)).containsKey("snippet")) {
				toret = (((List<Map<String, Object>>) response.get("organic_results")).get(0)).get("snippet")
					.toString();
			}
			else if ((((List<Map<String, Object>>) response.get("organic_results")).get(0)).containsKey("link")) {
				toret = (((List<Map<String, Object>>) response.get("organic_results")).get(0)).get("link").toString();
			}
			else if (response.containsKey("images_results")
					&& ((Map<String, Object>) ((List<Map<String, Object>>) response.get("images_results")).get(0))
						.containsKey("thumbnail")) {
				List<String> thumbnails = new ArrayList<>();
				List<Map<String, Object>> imageResults = (List<Map<String, Object>>) response.get("images_results");
				for (Map<String, Object> item : imageResults.subList(0, 10)) {
					thumbnails.add(item.get("thumbnail").toString());
				}
				toret = thumbnails.toString();
			}
			else {
				toret = "No good search result found";
			}
			log.warn("SerpapiTool result:{}", toret);
			this.lastSearchResults = toret;
			return new ToolExecuteResult(toret);
		}
		catch (Exception e) {
			log.error("Error deserializing JSON", e);
			return new ToolExecuteResult("Error deserializing JSON: " + e.getMessage());
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getParameters() {
		return PARAMETERS;
	}

	@Override
	public Class<GoogleSearchInput> getInputType() {
		return GoogleSearchInput.class;
	}

	@Override
	public boolean isReturnDirect() {
		return false;
	}

	@Override
	public ToolExecuteResult apply(GoogleSearchInput input, ToolContext toolContext) {
		return run(input);
	}

	public ToolExecuteResult run(GoogleSearchInput input) {
		String query = input.getQuery();
		Integer numResults = input.getNumResults() != null ? input.getNumResults() : 2;

		log.info("GoogleSearch input: query={}, numResults={}", query, numResults);

		this.lastQuery = query;
		this.lastNumResults = numResults;

		try {
			SerpApiService.Request request = new SerpApiService.Request(query);
			Map<String, Object> response = service.apply(request);

			if (response.containsKey("answer_box") && response.get("answer_box") instanceof List) {
				response.put("answer_box", ((List<Map<String, Object>>) response.get("answer_box")).get(0));
			}

			String toret = "";
			if (response.containsKey("answer_box")
					&& ((Map<String, Object>) response.get("answer_box")).containsKey("answer")) {
				toret = ((Map<String, Object>) response.get("answer_box")).get("answer").toString();
			}
			else if (response.containsKey("answer_box")
					&& ((Map<String, Object>) response.get("answer_box")).containsKey("snippet")) {
				toret = ((Map<String, Object>) response.get("answer_box")).get("snippet").toString();
			}
			else if (response.containsKey("answer_box")
					&& ((Map<String, Object>) response.get("answer_box")).containsKey("snippet_highlighted_words")) {
				toret = ((List<String>) ((Map<String, Object>) response.get("answer_box"))
					.get("snippet_highlighted_words")).get(0);
			}
			else if (response.containsKey("sports_results")
					&& ((Map<String, Object>) response.get("sports_results")).containsKey("game_spotlight")) {
				toret = ((Map<String, Object>) response.get("sports_results")).get("game_spotlight").toString();
			}
			else if (response.containsKey("shopping_results")
					&& ((List<Map<String, Object>>) response.get("shopping_results")).get(0).containsKey("title")) {
				List<Map<String, Object>> shoppingResults = (List<Map<String, Object>>) response
					.get("shopping_results");
				List<Map<String, Object>> subList = shoppingResults.subList(0, 3);
				toret = subList.toString();
			}
			else if (response.containsKey("knowledge_graph")
					&& ((Map<String, Object>) response.get("knowledge_graph")).containsKey("description")) {
				toret = ((Map<String, Object>) response.get("knowledge_graph")).get("description").toString();
			}
			else if ((((List<Map<String, Object>>) response.get("organic_results")).get(0)).containsKey("snippet")) {
				toret = (((List<Map<String, Object>>) response.get("organic_results")).get(0)).get("snippet")
					.toString();
			}
			else if ((((List<Map<String, Object>>) response.get("organic_results")).get(0)).containsKey("link")) {
				toret = (((List<Map<String, Object>>) response.get("organic_results")).get(0)).get("link").toString();
			}
			else if (response.containsKey("images_results")
					&& ((Map<String, Object>) ((List<Map<String, Object>>) response.get("images_results")).get(0))
						.containsKey("thumbnail")) {
				List<String> thumbnails = new ArrayList<>();
				List<Map<String, Object>> imageResults = (List<Map<String, Object>>) response.get("images_results");
				for (Map<String, Object> item : imageResults.subList(0, 10)) {
					thumbnails.add(item.get("thumbnail").toString());
				}
				toret = thumbnails.toString();
			}
			else {
				toret = "No good search result found";
			}
			log.warn("SerpapiTool result:{}", toret);
			this.lastSearchResults = toret;
			return new ToolExecuteResult(toret);
		}
		catch (Exception e) {
			log.error("Error executing Google search", e);
			return new ToolExecuteResult("Error executing Google search: " + e.getMessage());
		}
	}

	@Override
	public String getCurrentToolStateString() {
		return String.format("""
				Google Search Status:
				- Search Location: %s
				- Recent Search: %s
				- Search Results: %s
				""", new java.io.File("").getAbsolutePath(),
				lastQuery.isEmpty() ? "No search performed yet"
						: String.format("Searched for: '%s' (max results: %d)", lastQuery, lastNumResults),
				lastSearchResults.isEmpty() ? "No results found" : lastSearchResults);
	}

	@Override
	public void cleanup(String planId) {
		// do nothing
	}

	@Override
	public String getServiceGroup() {
		return "default-service-group";
	}

	// Implement the setPlanId method to satisfy the interface
	@Override
	public void setPlanId(String planId) {
		// No operation needed as planId is no longer used
	}

	/**
	 * 内部输入类，用于定义谷歌搜索工具的输入参数
	 */
	public static class GoogleSearchInput {

		private String query;

		@com.fasterxml.jackson.annotation.JsonProperty("num_results")
		private Integer numResults;

		public GoogleSearchInput() {
		}

		public GoogleSearchInput(String query, Integer numResults) {
			this.query = query;
			this.numResults = numResults;
		}

		public String getQuery() {
			return query;
		}

		public void setQuery(String query) {
			this.query = query;
		}

		public Integer getNumResults() {
			return numResults;
		}

		public void setNumResults(Integer numResults) {
			this.numResults = numResults;
		}

	}

}

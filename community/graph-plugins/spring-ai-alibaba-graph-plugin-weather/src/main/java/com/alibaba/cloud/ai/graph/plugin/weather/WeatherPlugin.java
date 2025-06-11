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
package com.alibaba.cloud.ai.graph.plugin.weather;

import com.alibaba.cloud.ai.graph.plugin.GraphPlugin;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.Map;

/**
 * Weather plugin that uses real WeatherAPI.com free API. To use this plugin, you need to
 * get a free API key from https://www.weatherapi.com/ and set it as an environment
 * variable: WEATHER_API_KEY
 */
public class WeatherPlugin implements GraphPlugin {

	private static final String WEATHER_API_BASE_URL = "https://api.weatherapi.com/v1";

	private static final String DEFAULT_API_KEY = "your_api_key_here"; // Replace with
																		// your actual API
																		// key

	private final WebClient webClient;

	private final ObjectMapper objectMapper;

	private final String apiKey;

	public WeatherPlugin() {
		this.webClient = WebClient.builder().baseUrl(WEATHER_API_BASE_URL).build();
		this.objectMapper = new ObjectMapper();
		// Try to get API key from environment variable, fallback to default
		this.apiKey = System.getenv("WEATHER_API_KEY") != null ? System.getenv("WEATHER_API_KEY") : DEFAULT_API_KEY;
	}

	@Override
	public String getId() {
		return "weather";
	}

	@Override
	public String getName() {
		return "Weather Service";
	}

	@Override
	public String getDescription() {
		return "Get real-time weather information for a location using WeatherAPI.com";
	}

	@Override
	public Map<String, Object> getInputSchema() {
		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "object");
		schema.put("required", new String[] { "location" });

		Map<String, Object> properties = new HashMap<>();
		Map<String, Object> location = new HashMap<>();
		location.put("type", "string");
		location.put("description", "City name or location (e.g., 'Beijing', 'New York', 'London')");
		properties.put("location", location);

		schema.put("properties", properties);
		return schema;
	}

	@Override
	public Map<String, Object> execute(Map<String, Object> params) throws Exception {
		String location = (String) params.get("location");
		if (location == null || location.trim().isEmpty()) {
			throw new IllegalArgumentException("Location parameter is required");
		}

		try {
			// Call WeatherAPI.com current weather endpoint
			String response = webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/current.json")
					.queryParam("key", apiKey)
					.queryParam("q", location.trim())
					.queryParam("aqi", "no") // Air quality data not needed
					.build())
				.retrieve()
				.bodyToMono(String.class)
				.block();

			if (response == null) {
				throw new RuntimeException("No response from weather API");
			}

			// Parse the JSON response
			Map<String, Object> apiResponse = objectMapper.readValue(response,
					new TypeReference<Map<String, Object>>() {
					});

			// Extract weather data from API response
			Map<String, Object> locationData = (Map<String, Object>) apiResponse.get("location");
			Map<String, Object> currentData = (Map<String, Object>) apiResponse.get("current");

			if (locationData == null || currentData == null) {
				throw new RuntimeException("Invalid response format from weather API");
			}

			// Format the response according to our schema
			Map<String, Object> result = new HashMap<>();
			result.put("location", locationData.get("name") + ", " + locationData.get("country"));
			result.put("region", locationData.get("region"));
			result.put("country", locationData.get("country"));
			result.put("temperature", currentData.get("temp_c"));
			result.put("temperature_f", currentData.get("temp_f"));
			result.put("condition", ((Map<String, Object>) currentData.get("condition")).get("text"));
			result.put("humidity", currentData.get("humidity"));
			result.put("wind_speed", currentData.get("wind_kph"));
			result.put("wind_direction", currentData.get("wind_dir"));
			result.put("pressure", currentData.get("pressure_mb"));
			result.put("visibility", currentData.get("vis_km"));
			result.put("uv_index", currentData.get("uv"));
			result.put("last_updated", currentData.get("last_updated"));

			return result;

		}
		catch (Exception e) {
			// Handle API errors gracefully
			if (e.getMessage().contains("No matching location found")) {
				throw new IllegalArgumentException(
						"Location '" + location + "' not found. Please check the spelling and try again.");
			}
			else if (e.getMessage().contains("API key")) {
				throw new RuntimeException("Invalid weather API key. Please set WEATHER_API_KEY environment variable.");
			}
			else {
				throw new RuntimeException("Failed to fetch weather data: " + e.getMessage(), e);
			}
		}
	}

}

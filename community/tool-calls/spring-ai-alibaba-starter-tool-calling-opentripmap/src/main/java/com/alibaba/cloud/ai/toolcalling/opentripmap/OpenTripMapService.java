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
package com.alibaba.cloud.ai.toolcalling.opentripmap;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * OpenTripMap service for searching places and getting tourist information.
 *
 * @author Makoto
 */
public class OpenTripMapService
		implements SearchService, Function<OpenTripMapService.Request, OpenTripMapService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(OpenTripMapService.class);

	private final WebClientTool webClientTool;

	private final JsonParseTool jsonParseTool;

	private final OpenTripMapProperties properties;

	public OpenTripMapService(WebClientTool webClientTool, JsonParseTool jsonParseTool,
			OpenTripMapProperties properties) {
		this.webClientTool = webClientTool;
		this.jsonParseTool = jsonParseTool;
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		// For simple search, we'll try to get coordinates first, then search places
		return CommonToolCallUtils.handleServiceError("OpenTripMap", () -> {
			// First try to get coordinates for the query location
			Request coordRequest = new Request(null, null, null, null, null, null, query, null, "coordinates");
			Response coordResponse = this.apply(coordRequest);

			if (coordResponse != null && coordResponse.coordinates != null) {
				try {
					// Parse coordinates and search for places
					Map<String, Object> coordData = jsonParseTool.jsonToObject(coordResponse.coordinates,
							new TypeReference<>() {
							});
					if (coordData.containsKey("lat") && coordData.containsKey("lon")) {
						Double lat = Double.valueOf(coordData.get("lat").toString());
						Double lon = Double.valueOf(coordData.get("lon").toString());

						Request searchRequest = new Request(lat, lon, 5000, 20, null, null, null, null, "search");
						Response searchResponse = this.apply(searchRequest);

						if (searchResponse != null && StringUtils.hasText(searchResponse.places)) {
							List<Map<String, Object>> places = jsonParseTool.jsonToObject(searchResponse.places,
									new TypeReference<>() {
									});
							List<SearchService.SearchContent> contents = new ArrayList<>();

							for (Map<String, Object> place : places) {
								String name = (String) place.get("name");
								String kinds = (String) place.get("kinds");
								contents.add(new SearchService.SearchContent(name != null ? name : "Unknown Place",
										kinds != null ? kinds : "Tourist attraction",
										"https://www.opentripmap.com/en/#13/" + lat + "/" + lon, null));
							}

							return () -> new SearchService.SearchResult(contents);
						}
					}
				}
				catch (Exception e) {
					logger.warn("Failed to parse coordinates: {}", e.getMessage());
				}
			}

			return () -> new SearchService.SearchResult(new ArrayList<>());
		}, logger);
	}

	@Override
	public Response apply(Request request) {
		if (CommonToolCallUtils.isInvalidateRequestParams(request)) {
			logger.error("Invalid request: request cannot be null");
			return null;
		}

		return CommonToolCallUtils.handleServiceError("OpenTripMap", () -> {
			String responseData;

			if ("coordinates".equals(request.operation) && StringUtils.hasText(request.placeName)) {
				// Get coordinates for place name
				responseData = getCoordinates(request.placeName);
				return new Response(null, null, responseData);
			}
			else if ("details".equals(request.operation) && StringUtils.hasText(request.xid)) {
				// Get place details
				responseData = getPlaceDetails(request.xid);
				return new Response(null, responseData, null);
			}
			else if ("search".equals(request.operation) && request.latitude != null && request.longitude != null) {
				// Search places around coordinates
				responseData = searchPlaces(request);
				return new Response(responseData, null, null);
			}
			else {
				logger.error("Invalid request: missing required parameters for operation {}", request.operation);
				return null;
			}
		}, logger);
	}

	private String searchPlaces(Request request) {
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("apikey", properties.getApiKey())
			.add("lon", String.valueOf(request.longitude))
			.add("lat", String.valueOf(request.latitude))
			.add("radius", String.valueOf(request.radius != null ? request.radius : 1000))
			.add("limit", String.valueOf(request.limit != null ? request.limit : 10))
			.add("format", "json")
			.build();

		if (StringUtils.hasText(request.kinds)) {
			params.add("kinds", request.kinds);
		}
		if (StringUtils.hasText(request.rate)) {
			params.add("rate", request.rate);
		}

		String response = webClientTool.get("/places/radius", params).block();
		logger.info("OpenTripMap places search completed for coordinates: {}, {}", request.latitude, request.longitude);
		return response;
	}

	private String getPlaceDetails(String xid) {
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("apikey", properties.getApiKey())
			.build();

		String response = webClientTool.get("/places/xid/" + xid, params).block();
		logger.info("OpenTripMap place details retrieved for xid: {}", xid);
		return response;
	}

	private String getCoordinates(String placeName) {
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("apikey", properties.getApiKey())
			.add("name", placeName)
			.build();

		String response = webClientTool.get("/places/geoname", params).block();
		logger.info("OpenTripMap coordinates retrieved for place: {}", placeName);
		return response;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("OpenTripMap service for searching places, getting place details, or finding coordinates.")
	public record Request(@JsonProperty(
			value = "latitude") @JsonPropertyDescription("Latitude coordinate for place search") Double latitude,

			@JsonProperty(
					value = "longitude") @JsonPropertyDescription("Longitude coordinate for place search") Double longitude,

			@JsonProperty(
					value = "radius") @JsonPropertyDescription("Search radius in meters (default: 1000, max: 50000)") Integer radius,

			@JsonProperty(
					value = "limit") @JsonPropertyDescription("Maximum number of results (default: 10, max: 500)") Integer limit,

			@JsonProperty(
					value = "kinds") @JsonPropertyDescription("Categories of places (e.g., 'museums', 'restaurants', 'hotels')") String kinds,

			@JsonProperty(
					value = "rate") @JsonPropertyDescription("Minimum rating (1-3, where 3 is highest)") String rate,

			@JsonProperty(
					value = "placeName") @JsonPropertyDescription("Place name to get coordinates for") String placeName,

			@JsonProperty(
					value = "xid") @JsonPropertyDescription("Unique place identifier for getting details") String xid,

			@JsonProperty(required = true,
					value = "operation") @JsonPropertyDescription("Operation type: 'search' (find places), 'details' (get place details), 'coordinates' (get coordinates for place name)") String operation)
			implements
				SearchService.Request {
		@Override
		public String getQuery() {
			return placeName != null ? placeName : (operation != null ? operation : "");
		}
	}

	@JsonClassDescription("OpenTripMap service response")
	public record Response(String places, String details, String coordinates) implements SearchService.Response {
		@Override
		public SearchService.SearchResult getSearchResult() {
			// This is mainly used for the SearchService interface
			return new SearchService.SearchResult(new ArrayList<>());
		}
	}

}

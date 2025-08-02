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
package com.alibaba.cloud.ai.toolcalling.tripadvisor;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * TripAdvisor Service
 *
 * @author Makoto
 */
public class TripAdvisorService
		implements SearchService, Function<TripAdvisorService.Request, TripAdvisorService.Response> {

	private static final Logger logger = LoggerFactory.getLogger(TripAdvisorService.class);

	private final JsonParseTool jsonParseTool;

	private final WebClientTool webClientTool;

	private final TripAdvisorProperties properties;

	public TripAdvisorService(JsonParseTool jsonParseTool, WebClientTool webClientTool,
			TripAdvisorProperties properties) {
		this.jsonParseTool = jsonParseTool;
		this.webClientTool = webClientTool;
		this.properties = properties;
	}

	@Override
	public SearchService.Response query(String query) {
		// For TripAdvisor, we'll use the query as a search term
		return this.apply(Request.searchLocations(properties.getApiKey(), query));
	}

	@Override
	public TripAdvisorService.Response apply(TripAdvisorService.Request request) {
		if (request == null) {
			return Response.errorResponse("request is null");
		}

		try {
			String endpoint;
			String responseData;

			if (StringUtils.hasText(request.locationId())) {
				// Get location details
				endpoint = "location/" + request.locationId() + "/details";
				MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
				params.add("key", request.apiKey());
				if (StringUtils.hasText(request.language())) {
					params.add("language", request.language());
				}
				if (StringUtils.hasText(request.currency())) {
					params.add("currency", request.currency());
				}
				responseData = webClientTool.get(endpoint, params).block();
			}
			else if (StringUtils.hasText(request.searchQuery())) {
				// Search locations
				endpoint = "location/search";
				MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
				params.add("key", request.apiKey());
				params.add("searchQuery", request.searchQuery());
				if (StringUtils.hasText(request.category())) {
					params.add("category", request.category());
				}
				if (StringUtils.hasText(request.phone())) {
					params.add("phone", request.phone());
				}
				if (StringUtils.hasText(request.address())) {
					params.add("address", request.address());
				}
				if (StringUtils.hasText(request.latLong())) {
					params.add("latLong", request.latLong());
				}
				if (StringUtils.hasText(request.radius())) {
					params.add("radius", request.radius());
				}
				if (StringUtils.hasText(request.radiusUnit())) {
					params.add("radiusUnit", request.radiusUnit());
				}
				if (StringUtils.hasText(request.language())) {
					params.add("language", request.language());
				}
				responseData = webClientTool.get(endpoint, params).block();
			}
			else {
				return Response.errorResponse("Either locationId or searchQuery must be provided");
			}

			return jsonParseTool.jsonToObject(responseData, new TypeReference<Response>() {
			});
		}
		catch (Exception ex) {
			logger.error("TripAdvisor API error: {}", ex.getMessage(), ex);
			return Response.errorResponse(ex.getMessage());
		}
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("This is the parameter entity class for TripAdvisorService; values must be strictly populated according to field requirements.")
	public record Request(@JsonProperty(value = "api_key",
			required = true) @JsonPropertyDescription("The API key for TripAdvisor Content API authentication.") String apiKey,

			@JsonProperty(
					value = "location_id") @JsonPropertyDescription("The unique TripAdvisor location ID to get details for. Use this for location details requests.") String locationId,

			@JsonProperty(
					value = "search_query") @JsonPropertyDescription("The search query string to find locations. Use this for search requests.") String searchQuery,

			@JsonProperty(
					value = "category") @JsonPropertyDescription("The category to filter search results. Options: hotels, attractions, restaurants, geos") String category,

			@JsonProperty(value = "phone") @JsonPropertyDescription("The phone number to search for.") String phone,

			@JsonProperty(value = "address") @JsonPropertyDescription("The address to search for.") String address,

			@JsonProperty(
					value = "lat_long") @JsonPropertyDescription("The latitude and longitude coordinates in format 'lat,long' to search around.") String latLong,

			@JsonProperty(
					value = "radius") @JsonPropertyDescription("The search radius when using latLong. Default is 25.") String radius,

			@JsonProperty(
					value = "radius_unit") @JsonPropertyDescription("The unit for the search radius. Options: km, mi. Default is km.") String radiusUnit,

			@JsonProperty(
					value = "language") @JsonPropertyDescription("The language code for localized content. Examples: en, zh, fr, de, es, it, ja") String language,

			@JsonProperty(
					value = "currency") @JsonPropertyDescription("The currency code for price information. Examples: USD, EUR, GBP, CNY") String currency)
			implements
				Serializable,
				SearchService.Request {

		public static Request locationDetails(String apiKey, String locationId) {
			return new Request(apiKey, locationId, null, null, null, null, null, null, null, "en", "USD");
		}

		public static Request searchLocations(String apiKey, String searchQuery) {
			return new Request(apiKey, null, searchQuery, null, null, null, null, null, null, "en", "USD");
		}

		@Override
		public String getQuery() {
			return this.searchQuery();
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Response(@JsonProperty("location_id") String locationId, @JsonProperty("name") String name,
			@JsonProperty("web_url") String webUrl, @JsonProperty("address_obj") AddressInfo addressObj,
			@JsonProperty("ancestors") List<AncestorInfo> ancestors, @JsonProperty("latitude") String latitude,
			@JsonProperty("longitude") String longitude, @JsonProperty("rating") String rating,
			@JsonProperty("rating_image_url") String ratingImageUrl, @JsonProperty("num_reviews") String numReviews,
			@JsonProperty("photo_count") String photoCount, @JsonProperty("write_review") String writeReview,
			@JsonProperty("location_string") String locationString, @JsonProperty("price_level") String priceLevel,
			@JsonProperty("category") CategoryInfo category,
			@JsonProperty("subcategory") List<CategoryInfo> subcategory, @JsonProperty("awards") List<AwardInfo> awards,
			@JsonProperty("ranking_data") RankingInfo rankingData, @JsonProperty("see_all_photos") String seeAllPhotos,
			@JsonProperty("data") List<LocationSearchResult> data,
			@JsonProperty("error") String error) implements SearchService.Response {

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record AddressInfo(@JsonProperty("street1") String street1, @JsonProperty("street2") String street2,
				@JsonProperty("city") String city, @JsonProperty("state") String state,
				@JsonProperty("country") String country, @JsonProperty("postalcode") String postalcode,
				@JsonProperty("address_string") String addressString) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record AncestorInfo(@JsonProperty("location_id") String locationId, @JsonProperty("name") String name,
				@JsonProperty("level") String level, @JsonProperty("abbrv") String abbrv) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record CategoryInfo(@JsonProperty("name") String name,
				@JsonProperty("localized_name") String localizedName) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record AwardInfo(@JsonProperty("award_type") String awardType, @JsonProperty("year") String year,
				@JsonProperty("display_name") String displayName) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record RankingInfo(@JsonProperty("ranking") String ranking,
				@JsonProperty("ranking_out_of") String rankingOutOf,
				@JsonProperty("ranking_string") String rankingString,
				@JsonProperty("geo_location_name") String geoLocationName) {
		}

		@JsonIgnoreProperties(ignoreUnknown = true)
		public record LocationSearchResult(@JsonProperty("location_id") String locationId,
				@JsonProperty("name") String name, @JsonProperty("address_obj") AddressInfo addressObj,
				@JsonProperty("rating") String rating, @JsonProperty("num_reviews") String numReviews) {
		}

		public static Response errorResponse(String errorMsg) {
			return new Response(null, null, null, null, null, null, null, null, null, null, null, null, null, null,
					null, null, null, null, null, null, errorMsg);
		}

		@Override
		public SearchResult getSearchResult() {
			if (this.data() != null && !this.data().isEmpty()) {
				return new SearchResult(this.data()
					.stream()
					.map(item -> new SearchService.SearchContent(item.name(),
							"Rating: " + item.rating() + ", Reviews: " + item.numReviews(), item.locationId(), null))
					.toList());
			}
			return new SearchResult(List.of());
		}
	}

}

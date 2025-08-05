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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = { TripAdvisorAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("TripAdvisor Test")
public class TripAdvisorServiceTest {

	@Autowired
	private TripAdvisorService tripAdvisorService;

	@Autowired
	private SearchService searchService;

	private static final Logger log = Logger.getLogger(TripAdvisorServiceTest.class.getName());

	@Test
	@DisplayName("Location Details Test")
	@EnabledIfEnvironmentVariable(named = TripAdvisorConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void locationDetailsTest() {
		String apiKey = System.getenv(TripAdvisorConstants.API_KEY_ENV);
		var request = TripAdvisorService.Request.locationDetails(apiKey, "89575"); // Boston
																					// Harbor
																					// Hotel
		var response = tripAdvisorService.apply(request);

		assert response != null;
		assert response.error() == null : "API returned error: " + response.error();
		assert response.name() != null : "Location name should not be null";
		assert response.locationId() != null : "Location ID should not be null";

		log.info("Location Details: " + response.name() + " (ID: " + response.locationId() + ")");
		log.info("Rating: " + response.rating() + " (" + response.numReviews() + " reviews)");
		log.info("Address: " + (response.addressObj() != null ? response.addressObj().addressString() : "N/A"));
	}

	@Test
	@DisplayName("Location Search Test")
	@EnabledIfEnvironmentVariable(named = TripAdvisorConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void locationSearchTest() {
		String apiKey = System.getenv(TripAdvisorConstants.API_KEY_ENV);
		var request = TripAdvisorService.Request.searchLocations(apiKey, "Times Square New York");
		var response = tripAdvisorService.apply(request);

		assert response != null;
		assert response.error() == null : "API returned error: " + response.error();
		assert response.data() != null && !response.data().isEmpty() : "Search should return results";

		log.info("Search Results Count: " + response.data().size());
		response.data().forEach(result -> {
			log.info("Found: " + result.name() + " (ID: " + result.locationId() + ", Rating: " + result.rating() + ")");
		});
	}

	@Test
	@DisplayName("Hotel Search Test")
	@EnabledIfEnvironmentVariable(named = TripAdvisorConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void hotelSearchTest() {
		String apiKey = System.getenv(TripAdvisorConstants.API_KEY_ENV);
		var request = new TripAdvisorService.Request(apiKey, null, "Boston hotels", "hotels", null, null, null, null,
				null, "en", "USD");
		var response = tripAdvisorService.apply(request);

		assert response != null;
		assert response.error() == null : "API returned error: " + response.error();

		log.info("Hotel Search Results: " + (response.data() != null ? response.data().size() : "0") + " hotels found");
	}

	@Test
	@DisplayName("Restaurant Search Test")
	@EnabledIfEnvironmentVariable(named = TripAdvisorConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void restaurantSearchTest() {
		String apiKey = System.getenv(TripAdvisorConstants.API_KEY_ENV);
		var request = new TripAdvisorService.Request(apiKey, null, "Italian restaurants Manhattan", "restaurants", null,
				null, null, null, null, "en", "USD");
		var response = tripAdvisorService.apply(request);

		assert response != null;
		assert response.error() == null : "API returned error: " + response.error();

		log.info("Restaurant Search Results: " + (response.data() != null ? response.data().size() : "0")
				+ " restaurants found");
	}

	@Test
	@DisplayName("Abstract Search Test")
	@EnabledIfEnvironmentVariable(named = TripAdvisorConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void abstractSearchTest() {
		var resp = searchService.query("Times Square New York");
		assert resp != null && resp.getSearchResult() != null && resp.getSearchResult().results() != null
				&& !resp.getSearchResult().results().isEmpty();
		log.info("Abstract Search Results: " + resp.getSearchResult());
	}

}

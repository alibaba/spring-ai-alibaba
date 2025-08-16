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

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallAutoConfiguration;
import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallConstants;
import com.alibaba.cloud.ai.toolcalling.common.interfaces.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.logging.Logger;

@SpringBootTest(classes = { OpenTripMapAutoConfiguration.class, CommonToolCallAutoConfiguration.class })
@DisplayName("OpenTripMap Test")
public class OpenTripMapServiceTest {

	@Autowired
	private OpenTripMapService openTripMapService;

	private static final Logger log = Logger.getLogger(OpenTripMapServiceTest.class.getName());

	@Test
	@DisplayName("Tool-Calling Coordinates Test")
	@EnabledIfEnvironmentVariable(named = OpenTripMapConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void openTripMapCoordinatesTest() {
		var request = new OpenTripMapService.Request(null, null, null, null, null, null, "London", null, "coordinates");
		var resp = openTripMapService.apply(request);
		assert resp != null && resp.coordinates() != null;
		log.info("coordinates result: " + resp.coordinates());
	}

	@Test
	@DisplayName("Tool-Calling Places Search Test")
	@EnabledIfEnvironmentVariable(named = OpenTripMapConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void openTripMapPlacesSearchTest() {
		// Using London coordinates: 51.5074, -0.1278
		var request = new OpenTripMapService.Request(51.5074, -0.1278, 2000, 5, "museums", null, null, null, "search");
		var resp = openTripMapService.apply(request);
		assert resp != null && resp.places() != null;
		log.info("places search result: " + resp.places());
	}

	@Test
	@DisplayName("Tool-Calling Place Details Test")
	@EnabledIfEnvironmentVariable(named = OpenTripMapConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void openTripMapPlaceDetailsTest() {
		// First get a place to get its xid, using a well-known xid for testing
		// This is a sample xid - in real usage you'd get this from a search first
		var request = new OpenTripMapService.Request(null, null, null, null, null, null, null, "N4020664", "details");
		var resp = openTripMapService.apply(request);
		// Note: This test might fail if the specific xid doesn't exist
		// In a real test, you'd first search for places to get valid xids
		log.info("place details result: " + (resp != null ? resp.details() : "null"));
	}

	@Autowired
	private SearchService searchService;

	@Test
	@DisplayName("Abstract Search Test")
	@EnabledIfEnvironmentVariable(named = OpenTripMapConstants.API_KEY_ENV,
			matches = CommonToolCallConstants.NOT_BLANK_REGEX)
	public void abstractSearchTest() {
		var resp = searchService.query("Paris");
		assert resp != null && resp.getSearchResult() != null;
		log.info("abstract search result: " + resp.getSearchResult());

		// Check if we got some results
		if (resp.getSearchResult().results() != null && !resp.getSearchResult().results().isEmpty()) {
			log.info("Found " + resp.getSearchResult().results().size() + " places");
			for (var content : resp.getSearchResult().results()) {
				log.info("Place: " + content.title() + " - " + content.content());
			}
		}
	}

}

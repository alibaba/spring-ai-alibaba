/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.plugin.crawler.util;

import org.junit.jupiter.api.Test;

import static com.alibaba.cloud.ai.plugin.crawler.util.UrlValidator.isValidUrl;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestUrlValidator {

	@Test
	public void testValidUrls() {

		assertTrue(isValidUrl("https://www.example.com"));
		assertTrue(isValidUrl("http://example.com/path"));
		assertTrue(isValidUrl("https://subdomain.example.com"));
		assertTrue(isValidUrl("http://192.168.1.1"));
		assertTrue(isValidUrl("http://example.com:8080/path?query=1"));
		assertTrue(isValidUrl("https://www.example.com:443"));
	}

	@Test
	public void testInvalidUrls() {

		assertFalse(isValidUrl("http://localhost"));
		assertFalse(isValidUrl("http://127.0.0.1"));
		assertFalse(isValidUrl("http://256.256.256.256"));
		assertFalse(isValidUrl("invalid-url"));
		assertFalse(isValidUrl("http://example..com"));
		assertFalse(isValidUrl("http:///example.com"));
		assertFalse(isValidUrl("ftp://example.com"));
	}

	@Test
	public void testEdgeCases() {

		assertTrue(isValidUrl("http://www.example.com/path/to/resource"));
		assertTrue(isValidUrl("https://example.com/path?query=value#fragment"));
		assertFalse(isValidUrl("http://example.com:99999"));
	}

}

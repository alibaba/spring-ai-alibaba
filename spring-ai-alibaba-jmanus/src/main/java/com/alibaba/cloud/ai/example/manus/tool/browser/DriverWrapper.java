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
package com.alibaba.cloud.ai.example.manus.tool.browser;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.Cookie;
import com.microsoft.playwright.options.SameSiteAttribute; // Import for SameSiteAttribute
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class DriverWrapper {

	private static final Logger log = LoggerFactory.getLogger(DriverWrapper.class);

	private Playwright playwright;

	private Page currentPage;

	private Browser browser;

	private InteractiveElementRegistry interactiveElementRegistry;

	private final Path cookiePath;

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// Mixin class for Playwright Cookie deserialization
	abstract static class PlaywrightCookieMixin {

		@JsonCreator
		PlaywrightCookieMixin(@JsonProperty("name") String name, @JsonProperty("value") String value) {
		}

		@JsonProperty("domain")
		abstract Cookie setDomain(String domain);

		@JsonProperty("path")
		abstract Cookie setPath(String path);

		@JsonProperty("expires")
		abstract Cookie setExpires(Number expires);

		@JsonProperty("httpOnly")
		abstract Cookie setHttpOnly(boolean httpOnly);

		@JsonProperty("secure")
		abstract Cookie setSecure(boolean secure);

		@JsonProperty("sameSite")
		abstract Cookie setSameSite(SameSiteAttribute sameSite);

	}

	static {
		objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
		// Register the mixin for Playwright Cookie class
		objectMapper.addMixIn(Cookie.class, PlaywrightCookieMixin.class);
	}

	public DriverWrapper(Playwright playwright, Browser browser, Page currentPage, String cookieDir) {
		this.playwright = playwright;
		this.currentPage = currentPage;
		this.browser = browser;
		this.interactiveElementRegistry = new InteractiveElementRegistry();

		if (cookieDir == null || cookieDir.trim().isEmpty()) {
			this.cookiePath = Paths.get("playwright-cookies-default.json");
			log.warn("Warning: cookieDir was not provided or was empty. Using default cookie path: {}",
					this.cookiePath.toAbsolutePath());
		}
		else {
			this.cookiePath = Paths.get(cookieDir, "playwright-cookies.json");
		}
		loadCookies();
	}

	private void loadCookies() {
		if (this.currentPage == null) {
			log.info("Cannot load cookies: currentPage is null.");
			return;
		}
		if (!Files.exists(this.cookiePath)) {
			log.info("Cookie file not found, skipping cookie loading: {}", this.cookiePath.toAbsolutePath());
			return;
		}
		try {
			byte[] jsonData = Files.readAllBytes(this.cookiePath);
			if (jsonData.length == 0) {
				log.info("Cookie file is empty, skipping cookie loading: {}", this.cookiePath.toAbsolutePath());
				return;
			}
			List<Cookie> cookies = objectMapper.readValue(jsonData, new TypeReference<List<Cookie>>() {
			});
			if (cookies != null && !cookies.isEmpty()) {
				this.currentPage.context().addCookies(cookies);
				log.info("Cookies loaded successfully from: {}", this.cookiePath.toAbsolutePath());
			}
			else {
				log.info("No cookies found in file or cookies list was empty: {}", this.cookiePath.toAbsolutePath());
			}
		}
		catch (IOException e) {
			log.info("Failed to load cookies from {}: {}", this.cookiePath.toAbsolutePath(), e.getMessage());
		}
		catch (Exception e) {
			log.info("An unexpected error occurred while loading cookies from {}: {}", this.cookiePath.toAbsolutePath(),
					e.getMessage());
		}
	}

	private void saveCookies() {
		if (this.currentPage == null) {
			log.info("Cannot save cookies: currentPage is null.");
			return;
		}
		try {
			List<Cookie> cookies = this.currentPage.context().cookies();
			if (cookies == null) {
				cookies = Collections.emptyList();
			}

			Path parentDir = this.cookiePath.getParent();
			if (parentDir != null && !Files.exists(parentDir)) {
				Files.createDirectories(parentDir);
			}

			byte[] jsonData = objectMapper.writeValueAsBytes(cookies);
			Files.write(this.cookiePath, jsonData);
			log.info("Cookies saved successfully to: {}", this.cookiePath.toAbsolutePath());
		}
		catch (IOException e) {
			log.info("Failed to save cookies to {}: {}", this.cookiePath.toAbsolutePath(), e.getMessage());
		}
		catch (Exception e) {
			log.info("An unexpected error occurred while saving cookies to {}: {}", this.cookiePath.toAbsolutePath(),
					e.getMessage());
		}
	}

	public InteractiveElementRegistry getInteractiveElementRegistry() {
		return interactiveElementRegistry;
	}

	public Playwright getPlaywright() {
		return playwright;
	}

	public void setPlaywright(Playwright playwright) {
		this.playwright = playwright;
	}

	public Page getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(Page currentPage) {
		this.currentPage = currentPage;
		// Potentially load cookies if page context changes and it's desired
		// loadCookies();
	}

	public Browser getBrowser() {
		return browser;
	}

	public void setBrowser(Browser browser) {
		this.browser = browser;
	}

	public void close() {
		saveCookies();
		if (this.currentPage != null) {
			try {
				this.currentPage.close();
			}
			catch (Exception e) {
				log.info("Error closing current page: {}", e.getMessage());
			}
		}
		if (this.browser != null) {
			try {
				this.browser.close();
			}
			catch (Exception e) {
				log.info("Error closing browser: {}", e.getMessage());
			}
		}
		// Playwright instance itself is usually managed by the service that created it.
		// Closing it here might be premature if other DriverWrappers use the same
		// Playwright instance.
		// if (this.playwright != null) {
		// try {
		// this.playwright.close();
		// } catch (Exception e) {
		// log.info("Error closing playwright: {}", e.getMessage());
		// }
		// }
	}

}

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

import com.microsoft.playwright.Playwright;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring Boot environment Playwright initializer Handles the special requirements for
 * running Playwright in Spring Boot fat jar
 */
@Component
public class SpringBootPlaywrightInitializer {

	private static final Logger log = LoggerFactory.getLogger(SpringBootPlaywrightInitializer.class);

	/**
	 * Initialize Playwright for Spring Boot environment
	 */
	public Playwright createPlaywright() {
		try {
			// Set up environment for Spring Boot
			setupSpringBootEnvironment();

			// Save current context class loader
			ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

			try {
				// Use this class's classloader (LaunchedClassLoader in Spring Boot)
				Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
				return Playwright.create();
			}
			finally {
				// Always restore original class loader
				Thread.currentThread().setContextClassLoader(originalClassLoader);
			}
		}
		catch (Exception e) {
			log.error("Failed to create Playwright in Spring Boot environment", e);
			throw new RuntimeException("Failed to initialize Playwright", e);
		}
	}

	/**
	 * Set up environment properties for Spring Boot
	 */
	private void setupSpringBootEnvironment() {
		// Set browser path
		String browserPath = System.getProperty("user.home") + "/.cache/ms-playwright";
		System.setProperty("playwright.browsers.path", browserPath);

		// Set driver temp directory
		String tempDir = System.getProperty("java.io.tmpdir");
		System.setProperty("playwright.driver.tmpdir", tempDir);

		// Check if browsers are installed
		Path browsersPath = Paths.get(browserPath);
		if (Files.exists(browsersPath)) {
			log.info("Playwright browsers found at: {}", browserPath);
			System.setProperty("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
		}
		else {
			log.warn("Playwright browsers not found at: {}. They will be downloaded on first use.", browserPath);
		}

		log.info("Spring Boot Playwright environment configured:");
		log.info("  - Browser path: {}", browserPath);
		log.info("  - Temp directory: {}", tempDir);
	}

	/**
	 * Check if Playwright can be initialized
	 */
	public boolean canInitialize() {
		try {
			// Try to find the required classes
			Class.forName("com.microsoft.playwright.Playwright");
			return true;
		}
		catch (ClassNotFoundException e) {
			log.error("Playwright classes not found in classpath", e);
			return false;
		}
	}

}

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
package com.alibaba.cloud.ai.manus.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import java.io.File;

/**
 * Playwright configuration for Docker environment Ensures Playwright uses the correct
 * browser path and doesn't download browsers
 */
@Configuration
public class PlaywrightConfig {

	private static final Logger log = LoggerFactory.getLogger(PlaywrightConfig.class);

	private final Environment environment;

	public PlaywrightConfig(Environment environment) {
		this.environment = environment;
	}

	@PostConstruct
	public void configurePlaywright() {
		// Set Playwright browser path for Docker environment
		String browsersPath = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
		if (browsersPath != null && !browsersPath.trim().isEmpty()) {
			System.setProperty("playwright.browsers.path", browsersPath);
			log.info("Set Playwright browsers path to: {}", browsersPath);

			// Verify the path exists and find chromium
			File browsersDir = new File(browsersPath);
			if (browsersDir.exists() && browsersDir.isDirectory()) {
				log.info("Playwright browsers directory exists: {}", browsersPath);
				findAndLogChromiumInstallation(browsersDir);
			}
			else {
				log.warn("Playwright browsers directory does not exist: {}", browsersPath);
			}
		}
		else {
			log.info("PLAYWRIGHT_BROWSERS_PATH not set, using default browser path");
		}

		// Set skip browser download flag for Docker environment
		String skipDownload = System.getenv("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD");
		if (skipDownload != null && skipDownload.equals("1")) {
			System.setProperty("playwright.skip.browser.download", "true");
			log.info("Set Playwright to skip browser download (browsers pre-installed)");
		}

		// Additional Docker-specific configurations
		if (isDockerEnvironment()) {
			log.info("Detected Docker environment, applying Docker-specific Playwright configurations");

			// Set additional Docker-specific properties
			System.setProperty("playwright.browser.headless", "true");
			System.setProperty("playwright.browser.no-sandbox", "true");
			System.setProperty("playwright.browser.disable-dev-shm-usage", "true");

			log.info("Applied Docker-specific Playwright configurations");
		}
	}

	/**
	 * Find and log chromium installation details
	 */
	private void findAndLogChromiumInstallation(File browsersDir) {
		File[] browserDirs = browsersDir.listFiles();
		if (browserDirs != null) {
			for (File browserDir : browserDirs) {
				if (browserDir.isDirectory()) {
					log.info("Found browser: {}", browserDir.getName());
					if (browserDir.getName().contains("chromium")) {
						searchForChromiumExecutable(browserDir);
					}
				}
			}
		}
	}

	/**
	 * Search for chromium executable in the browser directory
	 */
	private void searchForChromiumExecutable(File browserDir) {
		File[] subDirs = browserDir.listFiles();
		if (subDirs != null) {
			for (File subDir : subDirs) {
				if (subDir.isDirectory()) {
					log.info("  - Subdirectory: {}", subDir.getName());
					File[] files = subDir.listFiles();
					if (files != null) {
						for (File file : files) {
							if ((file.getName().contains("chrome") || file.getName().contains("headless_shell"))
									&& file.canExecute()) {
								log.info("    - Found executable: {}", file.getAbsolutePath());
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Check if running in Docker environment
	 */
	private boolean isDockerEnvironment() {
		// Check for Docker-specific environment variables or files
		String dockerEnv = System.getenv("DOCKER_ENV");
		if (dockerEnv != null && dockerEnv.equals("true")) {
			return true;
		}

		// Check if running in container by looking for .dockerenv file
		File dockerEnvFile = new File("/.dockerenv");
		if (dockerEnvFile.exists()) {
			return true;
		}

		// Check cgroup for Docker
		try {
			String cgroup = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("/proc/1/cgroup")));
			if (cgroup.contains("docker")) {
				return true;
			}
		}
		catch (Exception e) {
			// Ignore exceptions, not critical
		}

		return false;
	}

}

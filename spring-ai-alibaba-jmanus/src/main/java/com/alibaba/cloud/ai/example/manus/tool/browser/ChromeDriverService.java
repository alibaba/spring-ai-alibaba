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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import com.alibaba.cloud.ai.example.manus.tool.code.CodeUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;

import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ChromeDriverService {

	private static final Logger log = LoggerFactory.getLogger(ChromeDriverService.class);

	private final ConcurrentHashMap<String, DriverWrapper> drivers = new ConcurrentHashMap<>();

	private final Lock driverLock = new ReentrantLock();

	private ManusProperties manusProperties;

	// Initialize ObjectMapper instance
	private static final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * 共享目录 用来存cookies
	 */
	/**
	 * 共享目录 用来存cookies
	 */
	private String sharedDir;

	/**
	 * 获取当前共享目录
	 */
	public String getSharedDir() {
		return sharedDir;
	}

	/**
	 * 保存所有driver中的cookies到全局共享目录（cookies.json）
	 */
	public void saveCookiesToSharedDir() {
		// 取第一个可用 driver
		DriverWrapper driver = drivers.values().stream().findFirst().orElse(null);
		if (driver == null) {
			log.warn("No driver found for saving cookies");
			return;
		}
		try {
			List<com.microsoft.playwright.options.Cookie> cookies = driver.getCurrentPage().context().cookies();
			String cookieFile = sharedDir + "/cookies.json";
			try (java.io.FileWriter writer = new java.io.FileWriter(cookieFile)) {
				writer.write(objectMapper.writeValueAsString(cookies));
			}
			log.info("Cookies saved to {}", cookieFile);
		}
		catch (Exception e) {
			log.error("Failed to save cookies", e);
		}
	}

	/**
	 * 从全局共享目录加载cookies到所有driver
	 */
	public void loadCookiesFromSharedDir() {
		String cookieFile = sharedDir + "/cookies.json";
		java.io.File file = new java.io.File(cookieFile);
		if (!file.exists()) {
			log.warn("Cookie file does not exist: {}", cookieFile);
			return;
		}
		try (java.io.FileReader reader = new java.io.FileReader(cookieFile)) {
			// Replace FastJSON's JSON.parseArray with Jackson's objectMapper.readValue
			List<com.microsoft.playwright.options.Cookie> cookies = objectMapper.readValue(reader,
					new TypeReference<List<com.microsoft.playwright.options.Cookie>>() {
					});
			for (DriverWrapper driver : drivers.values()) {
				driver.getCurrentPage().context().addCookies(cookies);
			}
			log.info("Cookies loaded from {} to all drivers", cookieFile);
		}
		catch (Exception e) {
			log.error("Failed to load cookies for all drivers", e);
		}
	}

	public ChromeDriverService(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
		this.sharedDir = CodeUtils.getSharedDirectory(manusProperties.getBaseDir(), "playwright");
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("JVM shutting down - cleaning up Playwright processes");
			cleanupAllPlaywrightProcesses();
		}));
	}

	public DriverWrapper getDriver(String planId) {
		if (planId == null) {
			throw new IllegalArgumentException("planId cannot be null");
		}

		DriverWrapper currentDriver = drivers.get(planId);
		if (currentDriver != null) {
			return currentDriver;
		}

		try {
			driverLock.lock();
			currentDriver = drivers.get(planId);
			if (currentDriver != null) {
				return currentDriver;
			}
			log.info("Creating new Playwright Browser instance for planId: {}", planId);
			currentDriver = createNewDriver(); // createNewDriver will now pass sharedDir
			if (currentDriver != null) { // Check if driver creation was successful
				drivers.put(planId, currentDriver);
			}
			else {
				// Handle the case where driver creation failed, e.g., log an error or
				// throw an exception
				log.error("Failed to create new driver for planId: {}. createNewDriver returned null.", planId);
				// Optionally throw an exception to indicate failure to the caller
				// throw new RuntimeException("Failed to create new driver for planId: " +
				// planId);
			}
		}
		finally {
			driverLock.unlock();
		}

		return currentDriver;
	}

	private void cleanupAllPlaywrightProcesses() {
		try {
			drivers.clear();
			log.info("Successfully cleaned up all Playwright processes	");
		}
		catch (Exception e) {
			log.error("Error cleaning up Browser processes", e);
		}
	}

	public void closeDriverForPlan(String planId) {
		DriverWrapper driver = drivers.remove(planId);
		if (driver != null) {
			driver.close();
		}
	}

	private DriverWrapper createNewDriver() {
		Playwright playwright = null;
		try {

			if (playwright == null) {
				playwright = Playwright.create();
			}
			BrowserType.LaunchOptions options = new BrowserType.LaunchOptions();

			// 基础配置
			options.setArgs(Arrays.asList("--remote-allow-origins=*", "--disable-blink-features=AutomationControlled",
					"--disable-infobars", "--disable-notifications", "--disable-dev-shm-usage",
					"--lang=zh-CN,zh,en-US,en", "--user-agent=" + getRandomUserAgent(), "--window-size=1920,1080" // 默认窗口大小
			));

			// 根据配置决定是否使用 headless 模式
			if (manusProperties.getBrowserHeadless()) {
				log.info("启用 Playwright headless 模式");
				options.setHeadless(true);
			}
			else {
				log.info("启用 Playwright 非 headless 模式");
				options.setHeadless(false);
			}

			Browser browser = playwright.chromium().launch(options);
			log.info("Created new Playwright Browser instance with anti-detection");
			// Pass the sharedDir to the DriverWrapper constructor
			return new DriverWrapper(playwright, browser, browser.newPage(), this.sharedDir);
		}
		catch (Exception e) {
			if (playwright != null) {
				try {
					playwright.close();
				}
				catch (Exception ex) {
					log.warn("Failed to close failed Playwright instance", ex);
				}
			}
			log.error("Failed to create Playwright Browser instance", e);
			throw new RuntimeException("Failed to initialize Playwright Browser", e);
		}
	}

	private String getRandomUserAgent() {
		List<String> userAgents = Arrays.asList(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0");
		return userAgents.get(new Random().nextInt(userAgents.size()));
	}

	@PreDestroy
	public void cleanup() {
		log.info("Spring container shutting down - cleaning up Browser resources");
		cleanupAllPlaywrightProcesses();
	}

	public void setManusProperties(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
	}

	public ManusProperties getManusProperties() {
		return manusProperties;
	}

}

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

	public ChromeDriverService(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
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
			currentDriver = createNewDriver();
			drivers.put(planId, currentDriver);
		} finally {
			driverLock.unlock();
		}

		return currentDriver;
	}

	private void cleanupAllPlaywrightProcesses() {
		try {
			drivers.clear();
			log.info("Successfully cleaned up all Playwright processes	");
		} catch (Exception e) {
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
			options.setArgs(Arrays.asList(
					"--remote-allow-origins=*",
					"--disable-blink-features=AutomationControlled",
					"--disable-infobars",
					"--disable-notifications",
					"--disable-dev-shm-usage",
					"--lang=zh-CN,zh,en-US,en",
					"--user-agent=" + getRandomUserAgent(),
					"--window-size=1920,1080" // 默认窗口大小
			));

			// 根据配置决定是否使用 headless 模式
			if (manusProperties.getBrowserHeadless()) {
				log.info("启用 Playwright headless 模式");
				options.setHeadless(true);
			} else {
				log.info("启用 Playwright 非 headless 模式");
				options.setHeadless(false);
			}

			Browser browser = playwright.chromium().launch(options);
			log.info("Created new Playwright Browser instance with anti-detection");
			return new DriverWrapper(playwright, browser, browser.newPage());
		} catch (Exception e) {
			if (playwright != null) {
				try {
					playwright.close();
				} catch (Exception ex) {
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

}

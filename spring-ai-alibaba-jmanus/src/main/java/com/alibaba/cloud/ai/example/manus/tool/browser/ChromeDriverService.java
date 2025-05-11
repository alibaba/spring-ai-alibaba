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

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;
import com.alibaba.cloud.ai.example.manus.config.ManusProperties;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class ChromeDriverService implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(ChromeDriverService.class);

	private final ConcurrentHashMap<String, ChromeDriver> drivers = new ConcurrentHashMap<>();

	private final ManusProperties manusProperties;

	private final ConcurrentHashMap<String, Object> driverLocks = new ConcurrentHashMap<>();

	public ChromeDriverService(ManusProperties manusProperties) {
		this.manusProperties = manusProperties;
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("JVM shutting down - cleaning up Chrome processes");
			cleanupAllChromeProcesses();
		}));
	}

	private Object getDriverLock(String planId) {
		return driverLocks.computeIfAbsent(planId, k -> new Object());
	}

	public ChromeDriver getDriver(String planId) {
		if (planId == null) {
			throw new IllegalArgumentException("planId cannot be null");
		}

		ChromeDriver currentDriver = drivers.get(planId);
		if (currentDriver != null && isDriverActive(currentDriver)) {
			return currentDriver;
		}

		synchronized (getDriverLock(planId)) {
			currentDriver = drivers.get(planId);
			if (currentDriver != null && isDriverActive(currentDriver)) {
				return currentDriver;
			}

			ChromeDriver newDriver = createNewDriver();
			drivers.put(planId, newDriver);
			return newDriver;
		}
	}

	private void cleanupAllChromeProcesses() {
		try {
			// 关闭所有 driver
			for (Map.Entry<String, ChromeDriver> entry : drivers.entrySet()) {
				try {
					ChromeDriver driver = entry.getValue();
					if (driver != null) {
						closeDriver(driver);
					}
				}
				catch (Exception e) {
					log.error("Error closing ChromeDriver for planId: {}", entry.getKey(), e);
				}
			}
			drivers.clear();
			driverLocks.clear();
			log.info("Successfully cleaned up all Chrome drivers");
		}
		catch (Exception e) {
			log.error("Error cleaning up Chrome processes", e);
		}
	}

	public void closeDriverForPlan(String planId) {
		ChromeDriver driver = drivers.remove(planId);
		if (driver != null) {
			closeDriver(driver);
			driverLocks.remove(planId);
		}
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {

		Map<OsType, String> chromeDriverMap = checkOS();
		if (Objects.isNull(chromeDriverMap)) {
			throw new UnsupportedOperationException("不受支持的操作系统，当前仅支持 Windows、MacOS 和 Linux 系统");
		}

		String chromeDriverPath = getChromeDriverPath(chromeDriverMap);

		System.setProperty("webdriver.chrome.driver", chromeDriverPath);
		log.info("ChromeDriver path initialized: {}", chromeDriverPath);
	}

	private String getChromeDriverPath(Map<OsType, String> chromeDriverMap) throws IOException, URISyntaxException {
		if (chromeDriverMap.size() != 1) {
			throw new IllegalArgumentException("Chrome Driver Map 中的元素数量非法，必须且只能包含一个元素");
		}
		// 获取 ChromeDriver 的路径
		String chromeDriverPath = chromeDriverMap.values().iterator().next();

		// 获取资源 URL
		URL resource = getClass().getClassLoader().getResource(chromeDriverPath);
		if (resource == null) {
			throw new IllegalStateException("ChromeDriver not found: " + chromeDriverPath);
		}
		// 判断资源是否在 JAR 包中
		Path resolvedPath;
		if (resource.getProtocol().equals("jar")) {
			// 资源在 JAR 包中，需要提取到临时文件
			try (InputStream inputStream = resource.openStream()) {
				Path tempFile = Files.createTempFile("chromedriver", ".tmp");
				Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
				resolvedPath = tempFile;
				tempFile.toFile().deleteOnExit(); // 程序退出时删除临时文件
			}
		}
		else {
			// 资源在文件系统中，直接转换为 Path
			resolvedPath = Paths.get(resource.toURI());
		}
		// 返回绝对路径
		return resolvedPath.toAbsolutePath().toString();
	}

	private enum OsType {

		WINDOWS, MAC, LINUX, UNSUPPORTED

	}

	private static Map<OsType, String> checkOS() {

		String os = System.getProperty("os.name").toLowerCase();
		Map<OsType, String> resMap = new HashMap<>();

		if (os.contains("win")) {
			resMap.put(OsType.WINDOWS, "chromedriver/win64/chromedriver.exe");
		}
		else if (os.contains("mac")) {
			resMap.put(OsType.MAC, "chromedriver/mac-arm/chromedriver");
		}
		else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			resMap.put(OsType.LINUX, "chromedriver/linux64/chromedriver");
		}
		else {
			log.warn("不支持的操作系统类型: {}", os);
			return null;
		}

		return resMap;
	}

	private ChromeDriver createNewDriver() {
		ChromeDriver newDriver = null;
		try {
			ChromeOptions options = new ChromeOptions();

			// 基础配置
			options.addArguments("--remote-allow-origins=*");
			options.addArguments("--disable-blink-features=AutomationControlled");

			// 根据配置决定是否使用 headless 模式
			if (manusProperties.getBrowserHeadless()) {
				log.info("启用 Chrome headless 模式");
				options.addArguments("--headless=true");
			}

			// 模拟真实浏览器环境
			options.addArguments("--disable-infobars");
			options.addArguments("--disable-notifications");
			options.addArguments("--disable-dev-shm-usage");
			options.addArguments("--lang=zh-CN,zh,en-US,en");

			// 添加随机化的用户代理
			options.addArguments("--user-agent=" + getRandomUserAgent());

			// 添加随机化的浏览器窗口大小
			Dimension randomSize = getRandomWindowSize();
			options.addArguments("--window-size=" + randomSize.width + "," + randomSize.height);

			// 禁用自动化标志
			Map<String, Object> prefs = new HashMap<>();
			prefs.put("credentials_enable_service", false);
			prefs.put("profile.password_manager_enabled", false);
			options.setExperimentalOption("prefs", prefs);

			// 设置 webdriver 属性
			Map<String, Object> properties = new HashMap<>();
			properties.put("navigator.webdriver", false);
			options.setExperimentalOption("excludeSwitches", Arrays.asList("enable-automation"));

			// 自动匹配版本
			WebDriverManager.chromedriver().setup();
			newDriver = new ChromeDriver(options);
			executeAntiDetectionScript(newDriver);
			log.info("Created new ChromeDriver instance with anti-detection");
			return newDriver;
		}
		catch (Exception e) {
			if (newDriver != null) {
				try {
					newDriver.quit();
				}
				catch (Exception ex) {
					log.warn("Failed to quit failed driver instance", ex);
				}
			}
			log.error("Failed to create ChromeDriver instance", e);
			throw new RuntimeException("Failed to initialize ChromeDriver", e);
		}
	}

	private String getRandomUserAgent() {
		List<String> userAgents = Arrays.asList(
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
				"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
				"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0");
		return userAgents.get(new Random().nextInt(userAgents.size()));
	}

	private Dimension getRandomWindowSize() {
		List<Dimension> sizes = Arrays.asList(new Dimension(1920, 1080), new Dimension(1366, 768),
				new Dimension(1440, 900));
		return sizes.get(new Random().nextInt(sizes.size()));
	}

	private void executeAntiDetectionScript(WebDriver driver) {
		((JavascriptExecutor) driver).executeScript("""
				Object.defineProperty(navigator, 'webdriver', {
				    get: () => undefined
				});

				// 覆盖 navigator 属性
				const newProto = navigator.__proto__;
				delete newProto.webdriver;

				// 模拟真实的 plugins
				Object.defineProperty(navigator, 'plugins', {
				    get: () => [1, 2, 3, 4, 5],
				});

				// 模拟真实的语言
				Object.defineProperty(navigator, 'languages', {
				    get: () => ['zh-CN', 'zh', 'en-US', 'en'],
				});
				""");
	}

	private boolean isDriverActive(ChromeDriver driver) {
		try {
			driver.getCurrentUrl();
			return true;
		}
		catch (Exception e) {
			log.warn("Existing ChromeDriver is not active", e);
			closeDriver(driver);
			return false;
		}
	}

	private void closeDriver(WebDriver driver) {
		try {
			if (driver != null) {
				try {
					// 首先关闭所有窗口，但使用超时保护
					try {
						Set<String> windowHandles = driver.getWindowHandles();
						for (String handle : windowHandles) {
							try {
								driver.switchTo().window(handle);
								driver.close();
							}
							catch (Exception e) {
								log.warn("Error closing window: {}", e.getMessage());
							}
						}
					}
					catch (Exception e) {
						log.warn("Error getting window handles: {}", e.getMessage());
					}

					// 使用超时机制执行driver.quit()
					Thread shutdownThread = new Thread(() -> {
						try {
							driver.quit();
							log.info("ChromeDriver closed successfully");
						}
						catch (Exception e) {
							log.warn("Error in shutdown thread during driver.quit(): {}", e.getMessage());
						}
					}, "DriverShutdownThread");

					shutdownThread.setDaemon(true);
					shutdownThread.start();

					// 最多等待5秒
					shutdownThread.join(5000);

					if (shutdownThread.isAlive()) {
						log.warn("Shutdown thread timed out, may need to force kill browser processes");
					}
				}
				catch (InterruptedException ie) {
					log.warn("Driver shutdown thread interrupted: {}", ie.getMessage());
					Thread.currentThread().interrupt();
				}
			}
		}
		catch (Exception e) {
			log.error("Error closing ChromeDriver", e);
		}
	}

	@PreDestroy
	public void cleanup() {
		log.info("Spring container shutting down - cleaning up Chrome resources");
		cleanupAllChromeProcesses();
	}

}

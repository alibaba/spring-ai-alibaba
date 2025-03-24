package com.alibaba.cloud.ai.example.manus.service;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

import java.util.concurrent.atomic.AtomicReference;

@Service
@Primary
public class ChromeDriverService {
    private static final Logger log = LoggerFactory.getLogger(ChromeDriverService.class);
    private final AtomicReference<ChromeDriver> driver = new AtomicReference<>();

    public ChromeDriver getDriver() {

        return driver.updateAndGet(existing -> {
            if (existing != null && isDriverActive(existing)) {
                return existing;
            }
            return createNewDriver();
        });
    }

    private ChromeDriver createNewDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        // Add other options as needed
        // options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        // options.addArguments("--headless");
        // options.addArguments("--incognito");
        // options.addArguments("--no-sandbox");
        // options.addArguments("--disable-extensions");
        // options.addArguments("--start-maximized");
        ChromeDriver newDriver = new ChromeDriver(options);
        log.info("Created new ChromeDriver instance");
        return newDriver;
    }

    private boolean isDriverActive(ChromeDriver driver) {
        try {
            driver.getCurrentUrl();
            return true;
        } catch (Exception e) {
            log.warn("Existing ChromeDriver is not active", e);
            closeDriver(driver);
            return false;
        }
    }

    private void closeDriver(WebDriver driver) {
        try {
            if (driver != null) {
                driver.quit();
                log.info("ChromeDriver closed successfully");
            }
        } catch (Exception e) {
            log.error("Error closing ChromeDriver", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        ChromeDriver currentDriver = driver.get();
        if (currentDriver != null) {
            closeDriver(currentDriver);
            driver.set(null);
            log.info("ChromeDriver service cleaned up");
        }
    }
}

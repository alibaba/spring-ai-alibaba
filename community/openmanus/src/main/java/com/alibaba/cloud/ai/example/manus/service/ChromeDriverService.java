package com.alibaba.cloud.ai.example.manus.service;

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

import com.alibaba.cloud.ai.example.manus.config.BrowserProperties;
import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;

import jakarta.annotation.PreDestroy;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Primary
public class ChromeDriverService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(ChromeDriverService.class);
    private final AtomicReference<ChromeDriver> driver = new AtomicReference<>();
    private final BrowserProperties browserProperties;
    private static final String PID_FILE = "chrome-driver.pid";
    private File pidFile;

    public ChromeDriverService(BrowserProperties browserProperties) {
        this.browserProperties = browserProperties;
        this.pidFile = new File(System.getProperty("java.io.tmpdir"), PID_FILE);
        
        // 启动时清理可能存在的僵尸进程
        cleanupOrphanedProcesses();
        
        // 添加JVM关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("JVM shutting down - cleaning up all Chrome processes");
            cleanupAllChromeProcesses();
            deletePidFile();
        }));
    }

    private void cleanupOrphanedProcesses() {
        if (pidFile.exists()) {
            try {
                List<String> pids = Files.readAllLines(pidFile.toPath());
                for (String pid : pids) {
                    try {
                        killProcessByPid(pid.trim());
                    } catch (Exception e) {
                        log.warn("Failed to kill orphaned process with PID: {}", pid, e);
                    }
                }
            } catch (IOException e) {
                log.warn("Failed to read PID file", e);
            } finally {
                deletePidFile();
            }
        }
    }

    private void recordProcessId(String pid) {
        try {
            Files.write(pidFile.toPath(), 
                       Collections.singletonList(pid), 
                       StandardOpenOption.CREATE, 
                       StandardOpenOption.APPEND);
            log.info("Recorded Chrome process PID: {}", pid);
        } catch (IOException e) {
            log.error("Failed to record process PID", e);
        }
    }

    private void deletePidFile() {
        try {
            Files.deleteIfExists(pidFile.toPath());
        } catch (IOException e) {
            log.warn("Failed to delete PID file", e);
        }
    }

    private void killProcessByPid(String pid) {
        try {
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            String killCommand = isWindows ? "taskkill /F /PID " + pid : "kill -9 " + pid;
            Process process = Runtime.getRuntime().exec(killCommand);
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to kill process with PID: {}", pid, e);
        }
    }

    private String getProcessId(ChromeDriver driver) {
        try {
            String processName = driver.getClass().getName();
            ProcessHandle current = ProcessHandle.current();
            Optional<ProcessHandle> chromeProcess = ProcessHandle.allProcesses()
                .filter(process -> process.info().commandLine().map(cmd -> 
                    cmd.contains("chromedriver") || cmd.contains("chrome.exe"))
                    .orElse(false))
                .findFirst();
            
            return chromeProcess.map(handle -> String.valueOf(handle.pid()))
                              .orElse(null);
        } catch (Exception e) {
            log.warn("Failed to get process ID", e);
            return null;
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String chromeDriverPath = getChromeDriverPath(checkOS() ? 
            "data/chromedriver.exe" : "data/chromedriver");
        System.setProperty("webdriver.chrome.driver", chromeDriverPath);
        log.info("ChromeDriver path initialized: {}", chromeDriverPath);
    }

    private String getChromeDriverPath(String resourcePath) throws URISyntaxException {
        URL resource = OpenManusSpringBootApplication.class.getClassLoader().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("ChromeDriver not found: " + resourcePath);
        }
        return Paths.get(resource.toURI()).toFile().getAbsolutePath();
    }

    private static Boolean checkOS() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return true;
        }
        else if (os.contains("mac")) {
            return false;
        }
        else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            log.info("Operating System: Unix/Linux");
            return false;
        }
        else {
            log.info("Operating System: Unknown");
            return false;
        }
    }

    public ChromeDriver getDriver() {
        
        return driver.updateAndGet(existing -> {
        
            if (existing != null && isDriverActive(existing)) {
                return existing;
            }
            return createNewDriver();
        });
    }

    private ChromeDriver createNewDriver() {
        ChromeDriver newDriver = null;
        try {
            ChromeOptions options = new ChromeOptions();
            
            // 基础配置
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--disable-blink-features=AutomationControlled"); // 关键：禁用自动化控制检测
            
            // 根据配置决定是否使用 headless 模式
            if (browserProperties.isHeadless()) {
                log.info("启用 Chrome headless 模式");
                options.addArguments("--headless=new");
            }
            
            // 模拟真实浏览器环境
            options.addArguments("--disable-infobars");             // 禁用信息条
            options.addArguments("--disable-notifications");        // 禁用通知
            options.addArguments("--disable-dev-shm-usage");       // 禁用/dev/shm使用
            options.addArguments("--lang=zh-CN,zh,en-US,en");     // 设置语言
            
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
            
            newDriver = new ChromeDriver(options);
            String pid = getProcessId(newDriver);
            if (pid != null) {
                recordProcessId(pid);
            }
            
            executeAntiDetectionScript(newDriver);
            log.info("Created new ChromeDriver instance with anti-detection");
            return newDriver;
        } catch (Exception e) {
            if (newDriver != null) {
                try {
                    newDriver.quit();
                } catch (Exception ex) {
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
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0"
        );
        return userAgents.get(new Random().nextInt(userAgents.size()));
    }
    
    private Dimension getRandomWindowSize() {
        List<Dimension> sizes = Arrays.asList(
            new Dimension(1920, 1080),
            new Dimension(1366, 768),
            new Dimension(1440, 900)
        );
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
        } catch (Exception e) {
            log.warn("Existing ChromeDriver is not active", e);
            closeDriver(driver);
            return false;
        }
    }

    private void closeDriver(WebDriver driver) {
        try {
            if (driver != null) {
                // 首先关闭所有窗口
                Set<String> windowHandles = driver.getWindowHandles();
                for (String handle : windowHandles) {
                    try {
                        driver.switchTo().window(handle);
                        driver.close();
                    } catch (Exception e) {
                        log.warn("Error closing window: {}", e.getMessage());
                    }
                }
                // 然后退出driver
                driver.quit();
                log.info("ChromeDriver closed successfully");
            }
        } catch (Exception e) {
            log.error("Error closing ChromeDriver", e);
        }
    }

    private void cleanupAllChromeProcesses() {
        try {
            // 首先尝试正常关闭当前driver
            ChromeDriver currentDriver = driver.get();
            if (currentDriver != null) {
                closeDriver(currentDriver);
                driver.set(null);
            }

            // 使用系统命令清理所有相关进程
            boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
            if (isWindows) {
                Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe /T");
                Runtime.getRuntime().exec("taskkill /F /IM chrome.exe /T");
            } else {
                Runtime.getRuntime().exec("pkill -f chromedriver");
                Runtime.getRuntime().exec("pkill -f chrome");
            }
            
            // 清理PID文件
            deletePidFile();
            
            log.info("Successfully cleaned up all Chrome processes");
        } catch (Exception e) {
            log.error("Error cleaning up Chrome processes", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("Spring container shutting down - cleaning up Chrome resources");
        cleanupAllChromeProcesses();
    }
}

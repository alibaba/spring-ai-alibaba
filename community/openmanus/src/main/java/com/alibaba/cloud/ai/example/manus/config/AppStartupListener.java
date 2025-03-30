package com.alibaba.cloud.ai.example.manus.config;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * 应用启动监听器，在应用完全启动后打开浏览器访问首页
 */
@Component
public class AppStartupListener implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger logger = LoggerFactory.getLogger(AppStartupListener.class);

	@Value("${server.port:18080}")
	private String serverPort;

	@Value("${manus.openbrowser:true}")
	private boolean openBrowserAuto;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// 只有当配置允许自动打开浏览器时才执行
		if (!openBrowserAuto) {
			logger.info("自动打开浏览器功能已禁用");
			return;
		}

		String url = "http://localhost:" + serverPort + "/";
		logger.info("应用已启动，正在尝试打开浏览器访问: {}", url);

		// 首先尝试使用Desktop API
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(new URI(url));
				logger.info("已通过Desktop API成功打开浏览器");
				return;
			}
		}
		catch (Exception e) {
			logger.warn("使用Desktop API打开浏览器失败，尝试使用Runtime执行命令", e);
		}

		// 如果Desktop API失败，尝试使用Runtime执行命令
		String os = System.getProperty("os.name").toLowerCase();
		Runtime rt = Runtime.getRuntime();
		try {
			if (os.contains("mac")) {
				// macOS特定命令
				rt.exec(new String[] { "open", url });
				logger.info("已通过macOS open命令成功打开浏览器");
			}
			else if (os.contains("win")) {
				// Windows特定命令
				rt.exec(new String[] { "cmd", "/c", "start", url });
				logger.info("已通过Windows命令成功打开浏览器");
			}
			else if (os.contains("nix") || os.contains("nux")) {
				// Linux特定命令，尝试几种常见的浏览器打开方式
				String[] browsers = { "google-chrome", "firefox", "mozilla", "epiphany", "konqueror", "netscape",
						"opera", "links", "lynx" };

				StringBuilder cmd = new StringBuilder();
				for (int i = 0; i < browsers.length; i++) {
					if (i == 0) {
						cmd.append(String.format("%s \"%s\"", browsers[i], url));
					}
					else {
						cmd.append(String.format(" || %s \"%s\"", browsers[i], url));
					}
				}

				rt.exec(new String[] { "sh", "-c", cmd.toString() });
				logger.info("已通过Linux命令尝试打开浏览器");
			}
			else {
				logger.warn("未知操作系统，无法自动打开浏览器，请手动访问: {}", url);
			}
		}
		catch (IOException e) {
			logger.error("通过Runtime执行命令打开浏览器失败", e);
			logger.info("请手动在浏览器中访问: {}", url);
		}
	}

}

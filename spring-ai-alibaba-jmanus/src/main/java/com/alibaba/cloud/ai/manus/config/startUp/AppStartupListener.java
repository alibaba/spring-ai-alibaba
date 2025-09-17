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

package com.alibaba.cloud.ai.manus.config.startUp;

import com.alibaba.cloud.ai.manus.config.ManusProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.net.URI;

@Component
public class AppStartupListener implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger logger = LoggerFactory.getLogger(AppStartupListener.class);

	public static final String INIT_WEB_PATH = "/ui/index.html";

	@Value("${server.port:18080}")
	// Using Spring's original here to keep consistent with configuration file.
	private String serverPort;

	@Autowired
	private ManusProperties manusProperties;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		// Only execute when configuration allows auto-opening browser
		if (!manusProperties.getOpenBrowserAuto()) {
			logger.info("Auto-open browser feature is disabled");
			return;
		}

		String url = "http://localhost:" + serverPort + INIT_WEB_PATH;
		logger.info("Application started, attempting to open browser to access: {}", url);

		// First try using Desktop API
		try {
			if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
				Desktop.getDesktop().browse(new URI(url));
				logger.info("Successfully opened browser via Desktop API");
				return;
			}
		}
		catch (Throwable e) {
			logger.warn("Failed to open browser using Desktop API, trying Runtime command execution", e);
		}

		// If Desktop API fails, try using Runtime command execution
		String os = System.getProperty("os.name").toLowerCase();
		Runtime rt = Runtime.getRuntime();
		try {
			if (os.contains("mac")) {
				// macOS specific command
				rt.exec(new String[] { "open", url });
				logger.info("Successfully opened browser via macOS open command");
			}
			else if (os.contains("win")) {
				// Windows specific command
				rt.exec(new String[] { "cmd", "/c", "start", url });
				logger.info("Successfully opened browser via Windows command");
			}
			else if (os.contains("nix") || os.contains("nux")) {
				// Linux specific command, try several common browser opening methods
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
				logger.info("Attempted to open browser via Linux command");
			}
			else {
				logger.warn("Unknown operating system, cannot auto-open browser, please manually access: {}", url);
			}
		}
		catch (Throwable e) {
			logger.error("Failed to open browser via Runtime command execution", e);
			logger.info("Please manually access in browser: {}", url);
		}
	}

}

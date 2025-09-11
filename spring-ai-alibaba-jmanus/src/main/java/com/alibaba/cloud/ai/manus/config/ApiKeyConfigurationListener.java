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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyConfigurationListener implements ApplicationListener<ApplicationReadyEvent> {

	private static final Logger log = LoggerFactory.getLogger(ApiKeyConfigurationListener.class);

	@Autowired(required = false)
	private IConfigService configService;

	@Autowired
	private Environment environment;

	@Override
	public void onApplicationEvent(ApplicationReadyEvent event) {
		checkApiKeyConfiguration();
	}

	private void checkApiKeyConfiguration() {
		try {
			// Get Dynamic Api Key
			String apiKey = getDynamicApiKey();

			if (apiKey == null || apiKey.trim().isEmpty()) {
				log.warn("⚠️ Please remember to set the API key through the web interface at /ui/index.html");
				log.warn("    The system will not be able to use AI features until an API key is configured.");
			}
			else {
				log.info("✅ DashScope API key is configured and ready to use");
			}

		}
		catch (Exception e) {
			log.error("Failed to check API key configuration", e);
		}
	}

	private String getDynamicApiKey() {
		// Get from properties first
		if (configService != null) {
			try {
				String configValue = configService.getConfigValue("manus.dashscope.apiKey");
				if (configValue != null && !configValue.trim().isEmpty()) {
					return configValue.trim();
				}
			}
			catch (Exception e) {
				log.debug("Failed to get API key from config service", e);
			}
		}

		// If not present, use env property
		String envValue = environment.getProperty("DASHSCOPE_API_KEY");
		return envValue != null ? envValue.trim() : null;
	}

}

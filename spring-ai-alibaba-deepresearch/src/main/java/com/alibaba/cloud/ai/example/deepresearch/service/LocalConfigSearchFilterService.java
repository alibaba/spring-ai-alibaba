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

package com.alibaba.cloud.ai.example.deepresearch.service;

import com.alibaba.cloud.ai.example.deepresearch.util.SearchBeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Initializes website weight from local JSON configuration
 *
 * @author vlsmb
 * @since 2025/7/10
 */
@Service
public class LocalConfigSearchFilterService extends SearchFilterService {

	private static final Logger log = LoggerFactory.getLogger(LocalConfigSearchFilterService.class);

	private final ObjectMapper objectMapper = new ObjectMapper();

	public LocalConfigSearchFilterService(SearchBeanUtil searchBeanUtil) {
		super(searchBeanUtil);
	}

	@Override
	protected Map<String, Double> loadWebsiteWeight() {
		ClassPathResource resource = new ClassPathResource("website-weight-config.json");
		Map<String, Double> map = new HashMap<>();
		try (InputStream stream = resource.getInputStream()) {
			List<WebsiteConfig> configs = objectMapper.readValue(stream,
					objectMapper.getTypeFactory().constructCollectionType(List.class, WebsiteConfig.class));
			configs.forEach(config -> {
				if (config.weight() > 1.0) {
					log.warn("The weight field value for host '{}' is {}, exceeding the maximum weight limit.",
							config.host(), config.weight());
					map.put(config.host(), 1.0);
				}
				else if (config.weight() < -1.0) {
					log.warn("The weight field value for host '{}' is {}, exceeding the minimum weight limit.",
							config.host(), config.weight());
					map.put(config.host(), -1.0);
				}
				else {
					map.put(config.host(), config.weight());
				}
			});
		}
		catch (Exception e) {
			log.warn("Failed to read website weight configuration file: {}", e.getMessage());
			return Map.of();
		}
		return map;
	}

	private record WebsiteConfig(String host, Double weight) {

	}

}

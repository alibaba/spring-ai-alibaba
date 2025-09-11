
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

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Field;

@Configuration
public class JacksonConfig {

	private static final Logger logger = LoggerFactory.getLogger(JacksonConfig.class);

	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());

		// Configure UTF-8 support for proper Chinese character handling
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
		mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
		mapper.configure(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL, true);

		logger.info("Created ObjectMapper with JavaTimeModule and UTF-8 support registered");
		return mapper;
	}

	/**
	 * Fix Spring AI multiple tool classes ObjectMapper JavaTimeModule class loader issue
	 * Reference: https://github.com/spring-projects/spring-ai/issues/2921
	 */
	@PostConstruct
	public void fixSpringAiObjectMappers() {
		// Fix ModelOptionsUtils.OBJECT_MAPPER
		fixObjectMapperInClass("org.springframework.ai.model.ModelOptionsUtils", "OBJECT_MAPPER");

		// Fix JsonParser.OBJECT_MAPPER
		fixObjectMapperInClass("org.springframework.ai.util.json.JsonParser", "OBJECT_MAPPER");
	}

	private void fixObjectMapperInClass(String className, String fieldName) {
		try {
			Class<?> targetClass = Class.forName(className);
			Field objectMapperField = targetClass.getDeclaredField(fieldName);
			objectMapperField.setAccessible(true);

			ObjectMapper currentMapper = (ObjectMapper) objectMapperField.get(null);
			JavaTimeModule javaTimeModule = new JavaTimeModule();
			currentMapper.registerModule(javaTimeModule);

			logger.info("Successfully registered JavaTimeModule to {}.{}", className, fieldName);
		}
		catch (Exception e) {
			logger.error("Failed to modify {}.{}: {}", className, fieldName, e.getMessage(), e);
		}
	}

}

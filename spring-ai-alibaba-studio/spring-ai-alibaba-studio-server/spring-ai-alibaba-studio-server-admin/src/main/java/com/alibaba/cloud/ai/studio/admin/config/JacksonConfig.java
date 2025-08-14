package com.alibaba.cloud.ai.studio.admin.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * Configuration class for Jackson JSON processing
 */
@Configuration
public class JacksonConfig {

	/**
	 * Configures and returns a custom ObjectMapper bean - Excludes null values from
	 * serialization - Uses snake_case for property naming - Disables empty bean
	 * serialization failure - Enables timestamp format for dates - Ignores unknown
	 * properties during deserialization
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return Jackson2ObjectMapperBuilder.json()
			.serializationInclusion(JsonInclude.Include.NON_NULL)
			.propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
			.featuresToDisable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
			.featuresToEnable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

			// Configure deserialization features
			.featuresToDisable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
			.build();
	}

}

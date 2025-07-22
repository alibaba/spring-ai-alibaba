package com.alibaba.cloud.ai.studio.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for JWT token settings
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfigProperties {

	/**
	 * Access token expiration time in seconds
	 */
	private long accessTokenExpiration = 7200;

	/**
	 * Refresh token expiration time in seconds
	 */
	private long refreshTokenExpiration = 2592000;

}

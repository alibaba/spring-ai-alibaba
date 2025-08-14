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

package com.alibaba.cloud.ai.studio.core.base.manager;

import com.alibaba.cloud.ai.studio.core.config.JwtConfigProperties;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT token manager for handling authentication tokens. Manages the generation, storage,
 * and validation of access and refresh tokens.
 *
 * @since 1.0.0.3
 */
@Component
@RequiredArgsConstructor
public class TokenManager {

	/** JWT configuration properties */
	private final JwtConfigProperties jwtConfigProperties;

	/** Redis manager for token storage */
	private final RedisManager redisManager;

	/** Secret key for JWT signing */
	private final SecretKey key = Jwts.SIG.HS256.key().build();

	/**
	 * Generates a new access token for the given account ID
	 * @param accountId the account identifier
	 * @return the generated access token
	 */
	public String generateAccessToken(String accountId) {
		Map<String, Object> claims = new HashMap<>();
		String token = createToken(claims, accountId, jwtConfigProperties.getAccessTokenExpiration());

		storeAccessToken(token, accountId);
		return token;
	}

	/**
	 * Generates a new refresh token for the given account ID
	 * @param accountId the account identifier
	 * @return the generated refresh token
	 */
	public String generateRefreshToken(String accountId) {
		Map<String, Object> claims = new HashMap<>();
		String token = createToken(claims, accountId, jwtConfigProperties.getRefreshTokenExpiration());

		storeRefreshToken(token, accountId);
		return token;
	}

	/**
	 * Creates a JWT token with the specified claims and expiration
	 * @param claims token claims
	 * @param subject token subject
	 * @param expiration token expiration time in seconds
	 * @return the created JWT token
	 */
	private String createToken(Map<String, Object> claims, String subject, long expiration) {
		return Jwts.builder()
			.claims(claims)
			.subject(subject)
			.issuedAt(new Date(System.currentTimeMillis()))
			.expiration(new Date(System.currentTimeMillis() + expiration * 1000L))
			.signWith(key)
			.compact();
	}

	/**
	 * Stores the access token in Redis
	 * @param accessToken the access token to store
	 * @param accountId the associated account ID
	 */
	public void storeAccessToken(String accessToken, String accountId) {
		String key = getAccessTokenKey(accessToken);
		redisManager.put(key, accountId, Duration.ofSeconds(jwtConfigProperties.getAccessTokenExpiration()));
	}

	/**
	 * Retrieves the account ID associated with an access token
	 * @param accessToken the access token
	 * @return the associated account ID
	 */
	public String getAccountIdFromAccessToken(String accessToken) {
		String key = getAccessTokenKey(accessToken);
		return redisManager.get(key);
	}

	/**
	 * Deletes an access token from Redis
	 * @param accessToken the access token to delete
	 */
	public void deleteAccessToken(String accessToken) {
		String key = getAccessTokenKey(accessToken);
		redisManager.delete(key);
	}

	/**
	 * Stores the refresh token in Redis
	 * @param refreshToken the refresh token to store
	 * @param accountId the associated account ID
	 */
	public void storeRefreshToken(String refreshToken, String accountId) {
		String key = getRefreshTokenKey(refreshToken);
		redisManager.put(key, accountId, Duration.ofSeconds(jwtConfigProperties.getRefreshTokenExpiration()));
	}

	/**
	 * Retrieves the account ID associated with a refresh token
	 * @param refreshToken the refresh token
	 * @return the associated account ID
	 */
	public String getAccountIdFromRefreshToken(String refreshToken) {
		String key = getRefreshTokenKey(refreshToken);
		return redisManager.get(key);
	}

	/**
	 * Deletes a refresh token from Redis
	 * @param refreshToken the refresh token to delete
	 */
	public void deleteRefreshToken(String refreshToken) {
		String key = getRefreshTokenKey(refreshToken);
		redisManager.delete(key);
	}

	/**
	 * Generates the Redis key for an access token
	 * @param accessToken the access token
	 * @return the Redis key
	 */
	private String getAccessTokenKey(String accessToken) {
		return "access_token:" + accessToken;
	}

	/**
	 * Generates the Redis key for a refresh token
	 * @param refreshToken the refresh token
	 * @return the Redis key
	 */
	private String getRefreshTokenKey(String refreshToken) {
		return "refresh_token:" + refreshToken;
	}

}

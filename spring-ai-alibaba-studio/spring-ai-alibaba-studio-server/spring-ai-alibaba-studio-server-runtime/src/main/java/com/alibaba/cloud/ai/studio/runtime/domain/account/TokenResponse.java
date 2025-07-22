package com.alibaba.cloud.ai.studio.runtime.domain.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Response model for authentication token information
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TokenResponse implements Serializable {

	/**
	 * Access token for API authentication
	 */
	@JsonProperty("access_token")
	private String accessToken;

	/**
	 * Token used to refresh the access token
	 */
	@JsonProperty("refresh_token")
	private String refreshToken;

	/**
	 * Access token expiration time in unix timestamp
	 */
	@JsonProperty("expires_in")
	private Long expiresIn;

}

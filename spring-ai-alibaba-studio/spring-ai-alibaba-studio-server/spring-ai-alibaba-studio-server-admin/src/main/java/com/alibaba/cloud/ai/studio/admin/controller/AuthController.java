/*
 * Copyright 2024-2025 the original author or authors.
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
package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.constants.ApiConstants;
import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.account.LoginRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.account.RefreshTokenRequest;
import com.alibaba.cloud.ai.studio.runtime.domain.account.TokenResponse;
import com.alibaba.cloud.ai.studio.core.base.service.AccountService;
import com.alibaba.cloud.ai.studio.core.utils.common.IdGenerator;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for user authentication operations including login, token refresh, and
 * logout.
 */
@RestController
@Tag(name = "auth")
@RequestMapping("/console/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	/** Account service for handling authentication operations */
	private final AccountService accountService;

	/**
	 * Authenticates user and returns access tokens.
	 * @param loginRequest User credentials
	 * @return Access and refresh tokens
	 */
	@PostMapping("/login")
	public Result<TokenResponse> login(@RequestBody LoginRequest loginRequest) {
		if (StringUtils.isBlank(loginRequest.getUsername())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("username"));
		}

		if (StringUtils.isBlank(loginRequest.getPassword())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("password"));
		}

		TokenResponse response = accountService.login(loginRequest);
		return Result.success(IdGenerator.uuid(), response);
	}

	/**
	 * Refreshes access token using refresh token.
	 * @param request Refresh token request
	 * @return New access and refresh tokens
	 */
	@PostMapping("/refresh-token")
	public Result<TokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
		if (StringUtils.isBlank(request.getRefreshToken())) {
			throw new BizException(ErrorCode.INVALID_PARAMS.toError("refreshToken"));
		}

		TokenResponse response = accountService.refreshToken(request);
		return Result.success(IdGenerator.uuid(), response);
	}

	/**
	 * Invalidates user's access token.
	 * @param request HTTP request containing access token
	 * @return Success result
	 */
	@PostMapping("/logout")
	public Result<Void> logout(HttpServletRequest request) {
		String accessToken = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (StringUtils.isNotBlank(accessToken)) {
			accessToken = accessToken.replace(ApiConstants.TOKEN_PREFIX + " ", "");
		}

		accountService.logout(accessToken);
		return Result.success(IdGenerator.uuid(), null);
	}

}

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
package com.alibaba.cloud.ai.studio.core.base.service.impl;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.base.service.Oauth2Service;
import com.alibaba.cloud.ai.studio.core.config.OAuth2Config;
import com.alibaba.cloud.ai.studio.core.base.manager.HttpClientManager;
import com.alibaba.cloud.ai.studio.core.base.domain.RpcResult;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Oauth2Type;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Oauth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GitHubOAuth2ServiceImpl implements Oauth2Service {

	private final OAuth2Config config;

	private final HttpClientManager httpClientManager;

	public String getAuthorizationUrl() {
		return String.format("%s?client_id=%s&redirect_uri=%s&scope=user:email", config.getGithub().getAuthorizeUrl(),
				config.getGithub().getClientId(), config.getGithub().getRedirectUri());
	}

	public String getAccessToken(String code) {
		Map<String, Object> params = new HashMap<>();
		params.put("client_id", config.getGithub().getClientId());
		params.put("client_secret", config.getGithub().getClientSecret());
		params.put("code", code);
		params.put("redirect_uri", config.getGithub().getRedirectUri());

		RpcResult result = httpClientManager.doPostJson(config.getGithub().getTokenUrl(), params);
		if (!result.isSuccess()) {
			throw new BizException(ErrorCode.OAUTH2_CALL_ERROR.toError(), result.getMessage());
		}

		Map<String, Object> body = JsonUtils.fromJsonToMap(String.valueOf(result.getResponse()));
		if (body.get("error") != null) {
			throw new BizException(ErrorCode.OAUTH2_CALL_ERROR.toError(), String.valueOf(result.getResponse()));
		}

		return (String) body.get("access_token");
	}

	public Oauth2User getUserInfo(String accessToken) {
		Map<String, Object> headers = new HashMap<>();
		headers.put("Authorization", "Bearer " + accessToken);
		headers.put("Accept", "application/json");

		RpcResult result = httpClientManager.doGet(config.getGithub().getUserInfoUrl(), headers, null);
		if (!result.isSuccess()) {
			throw new BizException(ErrorCode.OAUTH2_CALL_ERROR.toError(), result.getMessage());
		}

		Map<String, Object> body = JsonUtils.fromJsonToMap(String.valueOf(result.getResponse()));
		Oauth2User user = new Oauth2User();
		user.setType(Oauth2Type.GITHUB);
		user.setId(body.get("id").toString());
		user.setUserId((String) body.get("login"));
		user.setName((String) body.get("name"));
		user.setEmail((String) body.get("email"));
		user.setIcon((String) body.get("avatar_url"));

		return user;
	}

}

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
package com.alibaba.cloud.ai.studio.core.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

@Configuration
@ConfigurationProperties(prefix = "oauth2")
@Data
public class OAuth2Config {

	private Github github;

	@Data
	public static class Github implements Serializable {

		private String clientId;

		private String clientSecret;

		private String redirectUri;

		private String authorizeUrl = "https://github.com/login/oauth/authorize";

		private String tokenUrl = "https://github.com/login/oauth/access_token";

		private String userInfoUrl = "https://api.github.com/user";

	}

}

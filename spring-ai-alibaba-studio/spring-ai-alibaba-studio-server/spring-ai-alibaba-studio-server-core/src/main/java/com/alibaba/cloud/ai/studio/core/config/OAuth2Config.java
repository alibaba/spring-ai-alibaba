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

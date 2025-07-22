package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.runtime.domain.account.TokenResponse;
import com.alibaba.cloud.ai.studio.core.base.service.AccountService;
import com.alibaba.cloud.ai.studio.core.base.service.Oauth2Service;
import com.alibaba.cloud.ai.studio.runtime.domain.account.Oauth2User;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class Oauth2Controller {

	private final Oauth2Service oauth2Service;

	private final AccountService accountService;

	@GetMapping("/login/github")
	public Result<String> githubLogin() {
		String authUrl = oauth2Service.getAuthorizationUrl();
		return Result.success(authUrl);
	}

	@GetMapping("/callback/github")
	public void githubCallback(@RequestParam(name = "code") String code, HttpServletResponse response)
			throws IOException {
		String accessToken = oauth2Service.getAccessToken(code);
		Oauth2User userInfo = oauth2Service.getUserInfo(accessToken);
		TokenResponse tokenResponse = accountService.login(userInfo);

		String redirectUrl = String.format("/?access_token=%s&refresh_token=%s&expires_in=%s",
				tokenResponse.getAccessToken(), tokenResponse.getRefreshToken(), tokenResponse.getExpiresIn());
		response.sendRedirect(redirectUrl);
	}

}

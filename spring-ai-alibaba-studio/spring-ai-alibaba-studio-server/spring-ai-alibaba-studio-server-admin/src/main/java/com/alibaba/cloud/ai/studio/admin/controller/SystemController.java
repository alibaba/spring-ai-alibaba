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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.alibaba.cloud.ai.studio.runtime.enums.UploadType;
import com.alibaba.cloud.ai.studio.runtime.domain.Result;
import com.alibaba.cloud.ai.studio.core.config.StudioProperties;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;

@Slf4j
@RestController
@Tag(name = "system")
@RequestMapping("/console/v1/system")
@RequiredArgsConstructor
public class SystemController {

	private final StudioProperties studioProperties;

	@GetMapping("/global-config")
	public Result<GlobalConfig> globalConfig() {
		GlobalConfig globalConfig = new GlobalConfig();
		if (StringUtils.isBlank(studioProperties.getLoginMethod())) {
			globalConfig.setLoginMethod(LoginMethodEnum.preset_account.name());
		}
		else {
			LoginMethodEnum loginMethodEnum = LoginMethodEnum.valueOf(studioProperties.getLoginMethod());
			globalConfig.setLoginMethod(loginMethodEnum.name());
		}

		if (StringUtils.isBlank(studioProperties.getUploadMethod())) {
			globalConfig.setUploadMethod(UploadType.FILE.name());
		}
		else {
			UploadType uploadMethodEnum = UploadType.fromValue(studioProperties.getUploadMethod());
			globalConfig.setUploadMethod(uploadMethodEnum.name().toLowerCase());
		}

		return Result.success(globalConfig);
	}

	@Data
	public static class GlobalConfig implements Serializable {

		@JsonProperty("login_method")
		private String loginMethod;

		@JsonProperty("upload_method")
		private String uploadMethod;

	}

	enum LoginMethodEnum {

		third_party, preset_account

	}

	@GetMapping("/health")
	public String health() {
		return "ok";
	}

}

/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.agent.studio.config;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WebConfigConditionalTest {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DispatcherServletAutoConfiguration.class, WebMvcAutoConfiguration.class,
				HttpMessageConvertersAutoConfiguration.class))
		.withUserConfiguration(WebConfig.class, TestController.class);

	@Test
	void shouldRegisterWebConfigWhenPropertyMissing() {
		this.contextRunner.run(context -> assertThat(context).hasSingleBean(WebConfig.class));
	}

	@Test
	void shouldRegisterWebConfigWhenEnabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.agent.studio.web.cors.enabled=true")
			.run(context -> assertThat(context).hasSingleBean(WebConfig.class));
	}

	@Test
	void shouldNotRegisterWebConfigWhenDisabled() {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.agent.studio.web.cors.enabled=false")
			.run(context -> assertThat(context).doesNotHaveBean(WebConfig.class));
	}

	@Test
	void shouldApplyCorsHeadersForAllowedOriginWhenEnabled() throws Exception {
		this.contextRunner.run(context -> {
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
			mockMvc.perform(options("/test")
					.header(ORIGIN, "http://localhost:3000")
					.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
				.andExpect(header().string(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
		});
	}

	@Test
	void shouldNotApplyCorsHeadersWhenDisabled() throws Exception {
		this.contextRunner.withPropertyValues("spring.ai.alibaba.agent.studio.web.cors.enabled=false").run(context -> {
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
			mockMvc.perform(options("/test")
					.header(ORIGIN, "http://localhost:3000")
					.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN))
				.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_CREDENTIALS));
		});
	}

	@Test
	void shouldNotApplyCorsHeadersForDisallowedOriginWhenEnabled() throws Exception {
		this.contextRunner.run(context -> {
			MockMvc mockMvc = MockMvcBuilders.webAppContextSetup((WebApplicationContext) context).build();
			mockMvc.perform(options("/test")
					.header(ORIGIN, "http://evil.example")
					.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN))
				.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_CREDENTIALS));
		});
	}

	@RestController
	static class TestController {

		@GetMapping("/test")
		String test() {
			return "ok";
		}

	}

}

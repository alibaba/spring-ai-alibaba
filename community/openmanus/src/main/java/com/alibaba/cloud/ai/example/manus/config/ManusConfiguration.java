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

package com.alibaba.cloud.ai.example.manus.config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.alibaba.cloud.ai.example.manus.agent.BaseAgent;
import com.alibaba.cloud.ai.example.manus.agent.ManusAgent;
import com.alibaba.cloud.ai.example.manus.flow.PlanningFlow;
import com.alibaba.cloud.ai.example.manus.llm.LlmService;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.util.Timeout;

import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Configuration
public class ManusConfiguration {

	@Bean
	public PlanningFlow planningFlow(LlmService llmService, ToolCallingManager toolCallingManager) {
		ManusAgent manusAgent = new ManusAgent(llmService, toolCallingManager);

		Map<String, BaseAgent> agentMap = new HashMap<>() {
			{
				put("manus", manusAgent);
			}
		};
		Map<String, Object> data = new HashMap<>();
		return new PlanningFlow(agentMap, data);
	}

	@Bean
	public RestClient.Builder createRestClient() {
		// 1. 配置超时时间（单位：毫秒）
		int connectionTimeout = 600000; // 连接超时时间
		int readTimeout = 600000; // 响应读取超时时间
		int writeTimeout = 600000; // 请求写入超时时间

		// 2. 创建 RequestConfig 并设置超时
		RequestConfig requestConfig = RequestConfig.custom()
			.setConnectTimeout(Timeout.of(10, TimeUnit.MINUTES)) // 设置连接超时
			.setResponseTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.setConnectionRequestTimeout(Timeout.of(10, TimeUnit.MINUTES))
			.build();

		// 3. 创建 CloseableHttpClient 并应用配置
		HttpClient httpClient = HttpClients.custom().setDefaultRequestConfig(requestConfig).build();

		// 4. 使用 HttpComponentsClientHttpRequestFactory 包装 HttpClient
		HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

		// 5. 创建 RestClient 并设置请求工厂
		return RestClient.builder().requestFactory(requestFactory);
	}

}

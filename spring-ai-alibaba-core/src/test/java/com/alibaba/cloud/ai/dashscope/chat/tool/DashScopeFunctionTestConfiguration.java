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
package com.alibaba.cloud.ai.dashscope.chat.tool;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Title Function Test configuration.<br>
 * Description Function Test configuration.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */
@Configuration
public class DashScopeFunctionTestConfiguration {

	@Bean
	public Function<MockWeatherService.Request, MockWeatherService.Response> weatherFunction() {
		MockWeatherService weatherService = new MockWeatherService();
		return (weatherService::apply);
	}

	@Bean
	@Description("根据用户编号和订单编号查询订单信息")
	public Function<MockOrderService.Request, MockOrderService.Response> getOrderFunction(
			MockOrderService mockOrderService) {
		return mockOrderService::getOrder;
	}

}

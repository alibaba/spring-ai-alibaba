/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.cloud.ai.dashscope.tool;

import com.alibaba.cloud.ai.dashscope.chat.tool.MockOrderService;
import com.alibaba.cloud.ai.dashscope.chat.tool.MockWeatherService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Description;

import java.util.function.Function;

/**
 * Title Function Test configuration.<br>
 * Description Function Test configuration.<br>
 * Created at 2024-09-03 10:17
 *
 * @author yuanci.ytb
 * @version 1.0.0
 * @since jdk8
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

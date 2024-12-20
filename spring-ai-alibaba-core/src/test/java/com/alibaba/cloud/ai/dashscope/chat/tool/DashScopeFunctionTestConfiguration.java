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

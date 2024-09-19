package com.alibaba.cloud.ai.example.functioncalling;

import java.util.function.Function;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootApplication
public class FunctionCallingExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(FunctionCallingExampleApplication.class, args);
	}

	@Bean
	@Description("根据用户编号和订单编号查询订单信息")
	public Function<MockOrderService.Request, MockOrderService.Response> getOrderFunction(
			MockOrderService mockOrderService) {
		return mockOrderService::getOrder;
	}

}

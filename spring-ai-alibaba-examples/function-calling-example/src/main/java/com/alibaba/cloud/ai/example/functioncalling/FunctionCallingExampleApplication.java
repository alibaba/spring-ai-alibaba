/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.cloud.ai.example.functioncalling;

import java.util.function.Function;

import com.alibaba.cloud.ai.example.functioncalling.entity.Response;
import com.alibaba.cloud.ai.example.functioncalling.function.MockOrderService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Description;

@SpringBootApplication
public class FunctionCallingExampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(FunctionCallingExampleApplication.class, args);
	}

	@Bean
	@Description("根据用户编号和订单编号查询订单信息")
	public Function<MockOrderService.Request, Response> getOrderFunction(
			MockOrderService mockOrderService) {

		return mockOrderService::getOrder;
	}

}

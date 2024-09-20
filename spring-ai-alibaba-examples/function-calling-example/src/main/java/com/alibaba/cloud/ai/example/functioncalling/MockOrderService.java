/*
 * All rights Reserved, Designed By Alibaba Group Inc.
 * Copyright: Copyright(C) 1999-2024
 * Company  : Alibaba Group Inc.
 */
package com.alibaba.cloud.ai.example.functioncalling;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import org.springframework.stereotype.Service;

/**
 * Title Mock order service.<br>
 * Description Mock order service.<br>
 * Created at 2024-09-03 10:15
 *
 * @author yuanci.ytb
 * @version 1.0.0
 * @since jdk8
 */

@Service
public class MockOrderService {

	public Response getOrder(Request request) {
		String productName = "尤尼克斯羽毛球拍";
		return new Response(String.format("%s的订单编号为%s, 购买的商品为: %s", request.userId, request.orderId, productName));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record Request(
			@JsonProperty(required = true,
					value = "orderId") @JsonPropertyDescription("订单编号, 比如1001***") String orderId,
			@JsonProperty(required = true,
					value = "userId") @JsonPropertyDescription("用户编号, 比如2001***") String userId) {
	}

	public record Response(String description) {
	}

}

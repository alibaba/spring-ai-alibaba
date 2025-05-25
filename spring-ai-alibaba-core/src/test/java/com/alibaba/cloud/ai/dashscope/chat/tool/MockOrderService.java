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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.springframework.stereotype.Service;

/**
 * Title Mock order service.<br>
 * Description Mock order service.<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
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

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

package com.alibaba.cloud.ai.studio.admin.controller;

import com.alibaba.cloud.ai.studio.runtime.exception.BizException;
import com.alibaba.cloud.ai.studio.runtime.enums.ErrorCode;
import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.core.utils.LogUtils;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Example API controller for testing purposes. Provides sample endpoints for
 * order-related operations.
 *
 * @since 1.0.0.3
 */
@RestController
@Tag(name = "test_example")
@RequestMapping("/test/api/example")
public class ApiExampleController {

	/**
	 * Retrieves order information via GET request. Returns a sample order with basic
	 * details and shipping information.
	 * @param headers HTTP request headers
	 * @param request HTTP request object
	 * @return Map containing order details
	 */
	@GetMapping("/getOrder")
	public Map<String, Object> getOrder(@RequestHeader Map<String, String> headers, HttpServletRequest request) {
		String uri = request.getRequestURI();
		Map<String, String[]> params = request.getParameterMap();

		LogUtils.info("ApiExampleController", "getOrder", uri, JsonUtils.toJson(headers), JsonUtils.toJson(params));

		Map<String, Object> result = new HashMap<>();
		result.put("orderId", "100001");
		result.put("description", "订单详情为尤尼克斯羽毛球拍");

		Map<String, Object> data = new HashMap<>();
		data.put("company", "顺丰快递");
		data.put("city", "苏州");
		result.put("data", data);

		return result;
	}

	/**
	 * Retrieves order information via POST request. Requires orderId in the request body.
	 * @param headers HTTP request headers
	 * @param body Request body containing order details
	 * @param request HTTP request object
	 * @return Map containing order details and items
	 * @throws BizException if orderId is missing
	 */
	@PostMapping("/getOrder")
	public Map<String, Object> getOrderWithPost(@RequestHeader Map<String, String> headers,
			@RequestBody Map<String, Object> body, HttpServletRequest request) {
		String uri = request.getRequestURI();
		Map<String, String[]> params = request.getParameterMap();

		LogUtils.info("ApiExampleController", "getOrderWithPost", uri, JsonUtils.toJson(headers),
				JsonUtils.toJson(params), JsonUtils.toJson(body));

		String orderId = (String) body.get("orderId");
		if (orderId == null) {
			throw new BizException(ErrorCode.MISSING_PARAMS.toError("orderId"));
		}

		Map<String, Object> result = new HashMap<>();
		result.put("orderId", orderId);
		result.put("description", "订单详情为尤尼克斯羽毛球拍");

		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> item = new HashMap<>();
		item.put("itemId", "2001");
		item.put("itemName", "羽毛球拍");
		item.put("price", 199.5);
		items.add(item);

		Map<String, Object> item2 = new HashMap<>();
		item.put("itemId", "2002");
		item.put("itemName", "亚狮龙7号");
		item.put("price", 99.5);
		items.add(item);

		result.put("items", items);

		return result;
	}

	/**
	 * Retrieves order information via POST request with orderId in path. Combines path
	 * variable and request body parameters.
	 * @param headers HTTP request headers
	 * @param orderId Order ID from path variable
	 * @param body Request body containing additional order details
	 * @param request HTTP request object
	 * @return Map containing order details and items
	 */
	@PostMapping("/getOrder/{orderId}")
	public Map<String, Object> getOrderWithPath(@RequestHeader Map<String, String> headers,
			@PathVariable("orderId") String orderId, @RequestBody Map<String, Object> body,
			HttpServletRequest request) {
		String uri = request.getRequestURI();
		Map<String, String[]> params = request.getParameterMap();

		LogUtils.info("ApiExampleController", "getOrderWithPost", uri, JsonUtils.toJson(headers),
				JsonUtils.toJson(params), JsonUtils.toJson(body));

		Map<String, Object> result = new HashMap<>();
		result.put("orderId", orderId);
		result.put("description", "订单详情为尤尼克斯羽毛球拍");

		List<Map<String, Object>> items = new ArrayList<>();
		Map<String, Object> item = new HashMap<>();
		item.put("itemId", "2001");
		item.put("itemName", "羽毛球拍");
		item.put("price", 199.5);
		items.add(item);

		result.put("items", items);

		return result;
	}

}

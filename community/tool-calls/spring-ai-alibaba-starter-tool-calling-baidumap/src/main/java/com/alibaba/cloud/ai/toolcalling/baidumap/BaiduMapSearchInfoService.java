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
package com.alibaba.cloud.ai.toolcalling.baidumap;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.function.Function;

public class BaiduMapSearchInfoService
		implements Function<BaiduMapSearchInfoService.Request, BaiduMapSearchInfoService.Response> {

	private final BaiDuMapTools baiduMapTools;

	@Override
	public Response apply(Request request) {
		return new Response(baiduMapTools.getAddressInformation(null, request.address(), true));
	}

	public BaiduMapSearchInfoService(BaiDuMapTools baiduMapTools) {
		this.baiduMapTools = baiduMapTools;
	}

	@JsonClassDescription("Get detail information of a address with baidu map")
	public record Request(@JsonProperty(required = true,
			value = "address") @JsonPropertyDescription("User-requested specific location address") String address) {
	}

	public record Response(String message) {
	}

}

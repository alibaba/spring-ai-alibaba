package com.alibaba.cloud.ai.toolcalling.baidumap;

import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
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

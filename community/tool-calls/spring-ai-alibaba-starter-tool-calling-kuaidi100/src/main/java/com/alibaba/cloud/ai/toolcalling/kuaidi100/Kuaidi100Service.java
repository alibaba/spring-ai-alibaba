/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *	  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.toolcalling.kuaidi100;

import com.alibaba.cloud.ai.toolcalling.common.CommonToolCallUtils;
import com.alibaba.cloud.ai.toolcalling.common.JsonParseTool;
import com.alibaba.cloud.ai.toolcalling.common.RestClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author XiaoYunTao
 * @author Allen Hu
 * @since 2024/12/25
 */
public class Kuaidi100Service implements Function<Kuaidi100Service.Request, Kuaidi100Service.QueryTrackResponse> {

	private static final Logger logger = LoggerFactory.getLogger(Kuaidi100Service.class);

	private final Kuaidi100Properties kuaidi100Properties;

	private final JsonParseTool jsonParseTool;

	private final RestClientTool restClientTool;

	public Kuaidi100Service(Kuaidi100Properties kuaidi100Properties, JsonParseTool jsonParseTool,
			RestClientTool restClientTool) {
		this.kuaidi100Properties = kuaidi100Properties;
		this.jsonParseTool = jsonParseTool;
		this.restClientTool = restClientTool;
	}

	@Override
	public Kuaidi100Service.QueryTrackResponse apply(Kuaidi100Service.Request request) {
		Kuaidi100Service.QueryTrackResponse queryTrackResp;
		try {
			String key = kuaidi100Properties.getApiKey();
			String customer = kuaidi100Properties.getAppId();
			String num = request.num();
			String company = queryCourierCompany(num, key);
			Assert.hasText(company, "Courier company not found.");
			queryTrackResp = queryCourierTrack(num, company, key, customer);
			logger.debug("queryTrackResp: {}", queryTrackResp);
		}
		catch (Exception e) {
			logger.error("Error occurred while querying track!", e);
			throw new Kuaidi100Exception("Error querying track.", e);
		}
		return queryTrackResp;
	}

	private String queryCourierCompany(String num, String key) throws Exception {
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("num", num)
			.add("key", key)
			.build();
		String body = restClientTool.get("autonumber/auto", params);
		try {
			List<QueryComResponse> queryComResponses = jsonParseTool.jsonToList(body, QueryComResponse.class);
			return !CollectionUtils.isEmpty(queryComResponses) ? queryComResponses.get(0).comCode() : null;
		}
		catch (JsonProcessingException e) {
			throw new Kuaidi100Exception(body, e);
		}
	}

	private QueryTrackResponse queryCourierTrack(String num, String com, String key, String customer) throws Exception {
		QueryTrackParam queryTrackParam = new QueryTrackParam(com, num);
		String param = jsonParseTool.objectToJson(queryTrackParam);
		String sign = DigestUtils.md5DigestAsHex((param + key + customer).getBytes()).toUpperCase();
		MultiValueMap<String, String> params = CommonToolCallUtils.<String, String>multiValueMapBuilder()
			.add("param", param)
			.add("customer", customer)
			.add("sign", sign)
			.build();
		String body = restClientTool.post("poll/query.do",
				CommonToolCallUtils.<String, String>multiValueMapBuilder().build(), Map.of(), params,
				MediaType.APPLICATION_FORM_URLENCODED);
		return jsonParseTool.jsonToObject(body, QueryTrackResponse.class);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Methods for real-time courier tracking")
	public record Request(
			@JsonProperty(required = true, value = "num") @JsonPropertyDescription("tracking number") String num) {
	}

	public record QueryComResponse(String lengthPre, String comCode, String noPre, String noCount) {
	}

	public record QueryTrackParam(String com, String num) {
	}

	@JsonClassDescription("Query courier tracking information")
	public record QueryTrackResponse(String message, String nu, String ischeck, String com, String status,
			List<QueryTrackData> data, String state, String condition, QueryTrackRouteInfo routeInfo, String returnCode,
			boolean result) {
	}

	public record QueryTrackData(String time, String context, String ftime, String areaCode, String areaName,
			String status, String areaCenter, String areaPinYin, String statusCode) {
	}

	public record QueryTrackRouteInfo(QueryTrackPosition from, QueryTrackPosition cur, QueryTrackPosition to) {
	}

	public record QueryTrackPosition(String number, String name) {
	}

}

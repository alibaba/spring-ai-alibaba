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
package com.alibaba.cloud.ai.toolcalling.kuaidi100;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.google.gson.Gson;
import com.kuaidi100.sdk.api.AutoNum;
import com.kuaidi100.sdk.api.QueryTrack;
import com.kuaidi100.sdk.request.AutoNumReq;
import com.kuaidi100.sdk.request.QueryTrackParam;
import com.kuaidi100.sdk.request.QueryTrackReq;
import com.kuaidi100.sdk.response.QueryTrackResp;
import com.kuaidi100.sdk.utils.SignUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author XiaoYunTao
 * @since 2024/12/25
 */
public class Kuaidi100Service implements Function<Kuaidi100Service.Request, QueryTrackResp> {

	private static final Logger logger = LoggerFactory.getLogger(Kuaidi100Service.class);

	private final Gson gson = new Gson();

	private final AutoNum autoNum = new AutoNum();

	final Kuaidi100Properties kuaidi100Properties;

	public Kuaidi100Service(Kuaidi100Properties kuaidi100Properties) {
		this.kuaidi100Properties = kuaidi100Properties;
	}

	@Override
	public QueryTrackResp apply(Kuaidi100Service.Request request) {
		QueryTrackResp queryTrackResp;
		try {
			queryTrackResp = queryTrack(request.num());
			logger.debug("queryTrackResp: {}", queryTrackResp);
		}
		catch (Exception e) {
			logger.error("Error occurred while querying track!", e);
			throw new Kuaidi100Exception("Error querying track.", e);
		}
		return queryTrackResp;
	}

	private QueryTrackResp queryTrack(String num) throws Exception {
		String key = kuaidi100Properties.getKey();
		String customer = kuaidi100Properties.getCustomer();

		QueryTrackParam queryTrackParam = createQueryTrackParam(num, key);
		String param = gson.toJson(queryTrackParam);

		QueryTrackReq queryTrackReq = createQueryTrackReq(customer, param, key);
		return new QueryTrack().queryTrack(queryTrackReq);
	}

	private QueryTrackParam createQueryTrackParam(String num, String key) throws Exception {
		AutoNumReq autoNumReq = new AutoNumReq();
		autoNumReq.setNum(num);
		autoNumReq.setKey(key);
		String company = autoNum.getFirstComByNum(autoNumReq);

		QueryTrackParam queryTrackParam = new QueryTrackParam();
		queryTrackParam.setCom(company);
		queryTrackParam.setNum(num);
		return queryTrackParam;
	}

	private QueryTrackReq createQueryTrackReq(String customer, String param, String key) {
		QueryTrackReq queryTrackReq = new QueryTrackReq();
		queryTrackReq.setParam(param);
		queryTrackReq.setCustomer(customer);
		queryTrackReq.setSign(SignUtils.querySign(param, key, customer));
		return queryTrackReq;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Methods for real-time courier tracking")
	public record Request(
			@JsonProperty(required = true, value = "num") @JsonPropertyDescription("tracking number") String num) {
	}

}
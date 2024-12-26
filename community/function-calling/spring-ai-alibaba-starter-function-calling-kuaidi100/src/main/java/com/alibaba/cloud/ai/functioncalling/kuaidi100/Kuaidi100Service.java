package com.alibaba.cloud.ai.functioncalling.kuaidi100;

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
 * @Author: XiaoYunTao
 * @Date: 2024/12/25
 */
public class Kuaidi100Service implements Function<Kuaidi100Service.Request, QueryTrackResp> {

	private static final Logger logger = LoggerFactory.getLogger(Kuaidi100Service.class);

	Kuaidi100Properties kuaidi100Properties;

	public Kuaidi100Service(Kuaidi100Properties kuaidi100Properties) {
		this.kuaidi100Properties = kuaidi100Properties;
	}

	@Override
	public QueryTrackResp apply(Kuaidi100Service.Request request) {
		QueryTrackResp queryTrackResp;
		try {
			queryTrackResp = queryTrack(request.num());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		logger.info("queryTrackResp: {}", queryTrackResp);
		return queryTrackResp;
	}

	public QueryTrackResp queryTrack(String num) throws Exception {
		String key = kuaidi100Properties.getKey();
		String customer = kuaidi100Properties.getCustomer();

		QueryTrackReq queryTrackReq = new QueryTrackReq();
		QueryTrackParam queryTrackParam = new QueryTrackParam();
		AutoNumReq autoNumReq = new AutoNumReq();
		autoNumReq.setNum(num);
		autoNumReq.setKey(key);
		AutoNum autoNum = new AutoNum();
		queryTrackParam.setCom(autoNum.getFirstComByNum(autoNumReq));
		queryTrackParam.setNum(num);
		String param = new Gson().toJson(queryTrackParam);

		queryTrackReq.setParam(param);
		queryTrackReq.setCustomer(customer);
		queryTrackReq.setSign(SignUtils.querySign(param, key, customer));
		return new QueryTrack().queryTrack(queryTrackReq);
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonClassDescription("Methods for real-time courier tracking")
	public record Request(
			@JsonProperty(required = true, value = "num") @JsonPropertyDescription("tracking number") String num) {
	}

}
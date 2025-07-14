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
package com.alibaba.cloud.ai.toolcalling.tushare;

import com.alibaba.cloud.ai.toolcalling.common.WebClientTool;
import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * @author HunterPorter
 */
public class TushareStockQuotesService
		implements Function<TushareStockQuotesService.Request, TushareStockQuotesService.Response> {

	private final WebClientTool webClientTool;

	private final TushareProperties tushareProperties;

	public TushareStockQuotesService(WebClientTool webClientTool, TushareProperties tushareProperties) {
		this.webClientTool = webClientTool;
		this.tushareProperties = tushareProperties;
	}

	/**
	 * 获取股票日行情
	 * @param tsCode 股票代码，例如000001.SZ
	 * @param startDate 开始日期，格式yyyyMMdd
	 * @param endDate 结束日期，格式yyyyMMdd
	 * @return https://tushare.pro/document/2?doc_id=27
	 */
	private String getStockQuotes(String tsCode, String startDate, String endDate) {
		try {
			Map<String, String> params = new HashMap<>(3);
			params.put("ts_code", tsCode);
			params.put("start_date", startDate);
			params.put("end_date", endDate);
			Map<String, Object> valueMap = Map.of("api_name", "daily", "token", tushareProperties.getToken(), "params",
					params, "fields", "ts_code,trade_date,open,high,low,close,pre_close,change,pct_chg,vol,amount");
			return webClientTool.post("", valueMap).block();
		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get stock quotes", e);
		}
	}

	@Override
	public Response apply(Request request) {
		try {
			return new Response(this.getStockQuotes(request.tsCode, request.startDate, request.endDate));
		}
		catch (Exception e) {
			return new Response("Error occurred while processing the request.");
		}
	}

	@JsonClassDescription("根据股票代码或日期获取股票日行情")
	public record Request(@JsonProperty(value = "ts_code") @JsonPropertyDescription("股票代码，例如000001.SZ") String tsCode,
			@JsonProperty(value = "start_date") @JsonPropertyDescription("开始日期，格式yyyyMMdd") String startDate,
			@JsonProperty(value = "end_date") @JsonPropertyDescription("结束日期，格式yyyyMMdd") String endDate) {
	}

	public record Response(String message) {
	}

}

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
package com.alibaba.cloud.ai.connector.bo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ResultSetBO extends DdlBaseBO implements Cloneable {

	private List<String> column;

	private List<Map<String, String>> data;

	private String errorMsg;

	@Override
	public ResultSetBO clone() {
		return ResultSetBO.builder().column(new ArrayList<>(this.column)).data(this.data.stream().map(x -> {
			HashMap<String, String> t = new HashMap<>();
			t.putAll(x);
			return t;
		}).collect(Collectors.toList())).build();
	}

	public List<String> getColumn() {
		return column;
	}

	public void setColumn(List<String> column) {
		this.column = column;
	}

	public List<Map<String, String>> getData() {
		return data;
	}

	public void setData(List<Map<String, String>> data) {
		this.data = data;
	}

	public String getErrorMsg() {
		return errorMsg;
	}

	public void setErrorMsg(String errorMsg) {
		this.errorMsg = errorMsg;
	}

	@Override
	public String toString() {
		return "ResultSetBO{" + "column=" + column + ", data=" + data + ", errorMsg='" + errorMsg + '\'' + '}';
	}

	public static ResultSetBOBuilder builder() {
		return new ResultSetBOBuilder();
	}

	public static final class ResultSetBOBuilder {

		private List<String> column;

		private List<Map<String, String>> data;

		private String errorMsg;

		private ResultSetBOBuilder() {
		}

		public ResultSetBOBuilder column(List<String> column) {
			this.column = column;
			return this;
		}

		public ResultSetBOBuilder data(List<Map<String, String>> data) {
			this.data = data;
			return this;
		}

		public ResultSetBOBuilder errorMsg(String errorMsg) {
			this.errorMsg = errorMsg;
			return this;
		}

		public ResultSetBO build() {
			ResultSetBO resultSetBO = new ResultSetBO();
			resultSetBO.setColumn(column);
			resultSetBO.setData(data);
			resultSetBO.setErrorMsg(errorMsg);
			return resultSetBO;
		}

	}

	/**
	 * Convert current object to JSON string
	 * @return JSON string
	 */
	public String toJsonStr() {
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to convert object to JSON string", e);
		}
	}

}

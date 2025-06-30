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
package com.alibaba.cloud.ai.dbconnector.bo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

}

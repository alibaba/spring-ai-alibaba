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
package com.alibaba.cloud.ai.toolcalling.regex;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * @author 北极星
 */
public class RegexService implements Function<RegexService.RegexRequest, Object> {

	/**
	 * Applies this function to the given argument.
	 * @param regexRequest the function argument
	 * @return the function result
	 */
	public java.lang.Object apply(RegexRequest regexRequest) {
		String content = regexRequest.content;
		Pattern expression = regexRequest.expression;
		int group = regexRequest.group;
		return RegexUtils.findAll(expression, content, group, new ArrayList<>());
	}

	public record RegexRequest(@JsonProperty("content") String content, @JsonProperty("expression") Pattern expression,
			@JsonProperty("group") int group) {
	}

}

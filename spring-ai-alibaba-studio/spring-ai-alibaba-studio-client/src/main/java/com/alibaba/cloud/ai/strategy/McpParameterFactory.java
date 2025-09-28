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

package com.alibaba.cloud.ai.strategy;

import com.alibaba.cloud.ai.common.McpTransportType;
import com.alibaba.cloud.ai.domain.McpParams;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class McpParameterFactory {

	private final Map<McpTransportType, ParameterParser<? extends McpParams>> parserMap;

	public McpParameterFactory(List<ParameterParser<? extends McpParams>> parsers) {
		parserMap = parsers.stream().collect(Collectors.toMap(ParameterParser::supportTransportType, p -> p));
	}

	@SuppressWarnings("unchecked")
	public <T extends McpParams> T parse(McpTransportType type, JsonNode node) {
		ParameterParser<? extends McpParams> parser = parserMap.get(type);
		if (parser == null) {
			throw new IllegalArgumentException("Unknown transport " + type);
		}
		try {
			return (T) parser.parse(node);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Parse error", e);
		}
	}

}

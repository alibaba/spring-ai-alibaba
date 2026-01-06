/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.tools;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

public class WeatherTool implements BiFunction<String, ToolContext, String> {
	public int counter = 0;

	public WeatherTool() {
	}

	@Override
	public String apply(
			@ToolParam(description = "The city or region to check the weather information." ) String location,
			ToolContext toolContext) {
		counter++;
		System.out.println("Weather tool called : " + location);
		return "Sunny, 25°C, light breeze from the east.";
	}

	public static ToolCallback createWeatherTool(String name, WeatherTool poetTool) {
		return FunctionToolCallback.builder(name, poetTool)
				.description("Weather information tool")
				.inputType(String.class)
				.build();
	}

}

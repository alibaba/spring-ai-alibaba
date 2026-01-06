/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.agent.tool;

import org.springframework.ai.model.ModelOptionsUtils;
import org.springframework.util.CollectionUtils;

import java.util.Map;

/**
 * Title tool arguments helper.<br>
 * Description tool arguments helper.<br>
 *
 * @since 1.0.0.3
 */

public class ToolArgumentsHelper {

	/**
	 * merge tool arguments with extraParams
	 * @param functionInput function input
	 * @param extraParams extra params
	 * @param toolId tool id
	 */
	public static Map<String, Object> mergeToolArguments(String functionInput, Map<String, Object> extraParams,
			String toolId) {
		Map<String, Object> arguments = ModelOptionsUtils.jsonToMap(functionInput);
		if (!CollectionUtils.isEmpty(extraParams) && extraParams.containsKey(toolId)) {
			Object obj = extraParams.get(toolId);
			if (obj instanceof Map) {
				Map<String, Object> map = (Map<String, Object>) obj;
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					if (!arguments.containsKey(entry.getKey())) {
						arguments.put(entry.getKey(), entry.getValue());
					}
				}
			}
			else {
				throw new IllegalArgumentException("extra tool param should be map");
			}
		}

		return arguments;
	}

}

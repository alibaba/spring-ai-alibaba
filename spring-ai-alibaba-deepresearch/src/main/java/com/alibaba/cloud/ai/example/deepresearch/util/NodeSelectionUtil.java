/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.example.deepresearch.util;

import com.alibaba.cloud.ai.example.deepresearch.config.AgenticConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * Get available routing nodes in a more flexible way
 *
 * @author ViliamSun
 * @since 1.0.0
 */
public class NodeSelectionUtil {

	private static Map<String, String> agentDescriptions;

	public static void init(Map<String, String> agentDescriptions) {
		NodeSelectionUtil.agentDescriptions = agentDescriptions;
	}

	/**
	 * Returns a collection of available nodes.
	 * @return a collection of node names
	 */
	public static Collection<String> getAvailableNodes() {
		return NodeSelectionUtil.agentDescriptions.keySet().stream().toList();
	}

}

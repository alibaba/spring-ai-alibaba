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

package com.alibaba.cloud.ai.example.deepresearch.tool.tavily;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author yingzi
 * @date 2025/5/18 15:24
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "spring.ai.tavily")
public class TavilySearchProperties {

	private String apiKey;

	private String topic = "general";

	private String searchDepth = "basic";

	private int maxResults = 5;

	private int chunksPerSource = 3;

	private int days = 7;

	private boolean includeRawContent = false;

	private boolean includeImages = false;

	private boolean includeImageDescriptions = false;

}

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

package com.alibaba.cloud.ai.example.deepresearch.config;

import com.alibaba.cloud.ai.example.deepresearch.model.BackgroundInvestigationType;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Allen Hu
 * @date 2025/5/24
 */
@ConfigurationProperties(prefix = "spring.ai.alibaba.deepreserch")
public class DeepResearchProperties {

	/**
	 * Set the type of background investigation node. Default is: just_web_search
	 */
	private BackgroundInvestigationType backgroundInvestigationType = BackgroundInvestigationType.JUST_WEB_SEARCH;

	public BackgroundInvestigationType getBackgroundInvestigationType() {
		return backgroundInvestigationType;
	}

	public void setBackgroundInvestigationType(BackgroundInvestigationType backgroundInvestigationType) {
		this.backgroundInvestigationType = backgroundInvestigationType;
	}

}

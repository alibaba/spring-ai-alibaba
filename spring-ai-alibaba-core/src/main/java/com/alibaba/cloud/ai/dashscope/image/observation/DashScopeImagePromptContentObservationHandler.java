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
package com.alibaba.cloud.ai.dashscope.image.observation;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import java.util.StringJoiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.image.observation.ImageModelObservationContext;
import org.springframework.util.CollectionUtils;

public class DashScopeImagePromptContentObservationHandler implements ObservationHandler<ImageModelObservationContext> {

	private static final Logger logger = LoggerFactory.getLogger(DashScopeImagePromptContentObservationHandler.class);

	@Override
	public void onStop(ImageModelObservationContext context) {
		if (!CollectionUtils.isEmpty(context.getRequest().getInstructions())) {
			StringJoiner joiner = new StringJoiner(", ", "[", "]");
			context.getRequest().getInstructions().forEach(msg -> joiner.add("\"" + msg.getText() + "\""));
			logger.info("DashScope image model Prompt : {}", joiner);
		}
	}

	@Override
	public boolean supportsContext(Observation.Context context) {
		return context instanceof ImageModelObservationContext;
	}

}

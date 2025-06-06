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

import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import io.micrometer.common.KeyValues;
import org.springframework.ai.image.observation.DefaultImageModelObservationConvention;
import org.springframework.ai.image.observation.ImageModelObservationContext;

public class DashScopeImageModelObservationConvention extends DefaultImageModelObservationConvention {

	@Override
	public KeyValues getHighCardinalityKeyValues(ImageModelObservationContext context) {
		var keyValues = super.getHighCardinalityKeyValues(context);

		var options = context.getRequest().getOptions();
		if (options instanceof DashScopeImageOptions dashOptions) {
			if (dashOptions.getRefImg() != null) {
				keyValues = keyValues.and("gen_ai.dashscope.ref_img", dashOptions.getRefImg());
			}
			if (dashOptions.getFunction() != null) {
				keyValues = keyValues.and("gen_ai.dashscope.function", dashOptions.getFunction());
			}
			if (dashOptions.getWatermark() != null) {
				keyValues = keyValues.and("gen_ai.dashscope.watermark", dashOptions.getWatermark().toString());
			}
		}

		return keyValues;
	}

	@Override
	public String getName() {
		return "dashscope.image.model.operation";
	}

}

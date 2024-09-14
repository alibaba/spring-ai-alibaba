/*
 * Copyright 2023-2024 the original author or authors.
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

package com.alibaba.cloud.ai.autoconfigure.dashscope;

import com.alibaba.cloud.ai.dashscope.image.DashScopeImageOptions;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * TongYi Image API properties.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @since 2023.0.1.0
 */

@ConfigurationProperties(DashScopeImageProperties.CONFIG_PREFIX)
public class DashScopeImageProperties extends DashScopeParentProperties {

	/**
	 * Spring Cloud Alibaba AI configuration prefix.
	 */
	public static final String CONFIG_PREFIX = "spring.ai.dashscope.image";

	/**
	 * Default TongYi Chat model.
	 */
	public static final String DEFAULT_IMAGES_MODEL_NAME = ImageSynthesis.Models.WANX_V1;

	/**
	 * Enable TongYiQWEN ai images client.
	 */
	private boolean enabled = true;

	@NestedConfigurationProperty
	private DashScopeImageOptions options = DashScopeImageOptions.builder()
		.withModel(DEFAULT_IMAGES_MODEL_NAME)
		.withN(1)
		.build();

	public DashScopeImageOptions getOptions() {

		return this.options;
	}

	public void setOptions(DashScopeImageOptions options) {

		this.options = options;
	}

	public boolean isEnabled() {

		return this.enabled;
	}

	public void setEnabled(boolean enabled) {

		this.enabled = enabled;
	}

}

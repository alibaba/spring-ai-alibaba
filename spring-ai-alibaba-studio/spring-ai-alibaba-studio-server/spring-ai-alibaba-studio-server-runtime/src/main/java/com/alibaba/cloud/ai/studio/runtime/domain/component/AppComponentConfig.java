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

package com.alibaba.cloud.ai.studio.runtime.domain.component;

import com.alibaba.cloud.ai.studio.runtime.enums.APIPluginValueSourceEnum;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for application components
 *
 * @author guning.lt
 * @since 1.0.0.3
 */
@Data
public class AppComponentConfig implements Serializable {

	/** Input configuration for the component */
	private Input input;

	/** List of output parameters */
	private List<Params> output = new ArrayList<>();

	@Data
	public static class Input {

		/** User-defined parameters */
		private List<UserParams> userParams = new ArrayList<>();

		/** System-defined parameters */
		private List<Params> systemParams = new ArrayList<>();

	}

	@Data
	public static class UserParams {

		/** Parameter code */
		private String code;

		/** Parameter name */
		private String name;

		/** List of parameter configurations */
		private List<Params> params = new ArrayList<>();

	}

	@Data
	public static class Params {

		/** Field identifier */
		private String field;

		/** Parameter description */
		private String description;

		/** Parameter type */
		private String type;

		/** Whether the parameter is required */
		private Boolean required = false;

		/** Whether to display the parameter */
		private Boolean display = true;

		/** Default value for the parameter */
		private Object defaultValue;

		/** Alternative name for the parameter */
		private String alias;

		/** Source of the parameter value */
		private String source = APIPluginValueSourceEnum.MODEL.getCode();

	}

}

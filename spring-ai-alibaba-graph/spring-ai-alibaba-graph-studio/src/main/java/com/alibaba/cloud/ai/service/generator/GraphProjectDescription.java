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
package com.alibaba.cloud.ai.service.generator;

import com.alibaba.cloud.ai.model.AppModeEnum;
import io.spring.initializr.generator.project.MutableProjectDescription;

/**
 * Custom ProjectDescription for Graph Project
 *
 * @author robocanic
 * @since 2025/5/18
 */
public class GraphProjectDescription extends MutableProjectDescription {

	private String dsl;

	private AppModeEnum appMode;

	public GraphProjectDescription() {
	}

	public GraphProjectDescription(GraphProjectDescription source) {
		super(source);
		this.dsl = source.dsl;
		this.appMode = source.appMode;
	}

	@Override
	public MutableProjectDescription createCopy() {
		return new GraphProjectDescription(this);
	}

	public String getDsl() {
		return dsl;
	}

	public void setDsl(String dsl) {
		this.dsl = dsl;
	}

	public AppModeEnum getAppMode() {
		return appMode;
	}

	public void setAppMode(AppModeEnum appMode) {
		this.appMode = appMode;
	}

}

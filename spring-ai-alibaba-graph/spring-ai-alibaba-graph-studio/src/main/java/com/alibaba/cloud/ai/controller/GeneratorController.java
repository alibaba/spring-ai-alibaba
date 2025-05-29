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
package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.service.generator.GraphProjectRequest;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import io.spring.initializr.web.controller.ProjectGenerationController;
import io.spring.initializr.web.project.ProjectGenerationInvoker;
import org.springframework.beans.BeanWrapperImpl;

import java.util.Map;

public class GeneratorController extends ProjectGenerationController<GraphProjectRequest> {

	public GeneratorController(InitializrMetadataProvider metadataProvider,
			ProjectGenerationInvoker<GraphProjectRequest> projectGenerationInvoker) {
		super(metadataProvider, projectGenerationInvoker);
	}

	@Override
	public GraphProjectRequest projectRequest(Map<String, String> headers) {
		GraphProjectRequest request = new GraphProjectRequest();
		BeanWrapperImpl bean = new BeanWrapperImpl(this);
		getMetadata().defaults().forEach((key, value) -> {
			if (bean.isWritableProperty(key)) {
				// We want to be able to infer a package name if none has been
				// explicitly set
				if (!key.equals("packageName")) {
					bean.setPropertyValue(key, value);
				}
			}
		});
		return request;
	}

}

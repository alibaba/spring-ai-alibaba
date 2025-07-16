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
package com.alibaba.cloud.ai.config;

import com.alibaba.cloud.ai.controller.GeneratorController;
import com.alibaba.cloud.ai.format.EclipseJdtFormatProjectContributor;
import com.alibaba.cloud.ai.service.generator.GraphAppPropertiesCustomizer;
import com.alibaba.cloud.ai.service.generator.GraphProjectReqToDescConverter;
import com.alibaba.cloud.ai.service.generator.GraphProjectRequest;
import io.spring.initializr.generator.project.contributor.ProjectContributor;
import io.spring.initializr.generator.spring.properties.ApplicationPropertiesCustomizer;
import io.spring.initializr.metadata.InitializrMetadataProvider;
import io.spring.initializr.web.controller.ProjectGenerationController;
import io.spring.initializr.web.project.ProjectGenerationInvoker;
import io.spring.initializr.web.project.DefaultProjectRequestPlatformVersionTransformer;
import io.spring.initializr.web.project.ProjectRequestPlatformVersionTransformer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GraphInitializrConfiguration {

	@Bean
	@ConditionalOnMissingBean
	ProjectGenerationController<GraphProjectRequest> projectGenerationController(
			InitializrMetadataProvider metadataProvider,
			ObjectProvider<ProjectRequestPlatformVersionTransformer> platformVersionTransformer,
			ApplicationContext applicationContext) {
		ProjectGenerationInvoker<GraphProjectRequest> projectGenerationInvoker = new ProjectGenerationInvoker<>(
				applicationContext, new GraphProjectReqToDescConverter(platformVersionTransformer
					.getIfAvailable(DefaultProjectRequestPlatformVersionTransformer::new)));
		return new GeneratorController(metadataProvider, projectGenerationInvoker);
	}

	@Bean
	public ProjectContributor eclipseJdtFormatContributor() {
		return new EclipseJdtFormatProjectContributor();
	}

	@Bean
	public ApplicationPropertiesCustomizer graphAppPropertiesCustomizer() {
		return new GraphAppPropertiesCustomizer();
	}

}

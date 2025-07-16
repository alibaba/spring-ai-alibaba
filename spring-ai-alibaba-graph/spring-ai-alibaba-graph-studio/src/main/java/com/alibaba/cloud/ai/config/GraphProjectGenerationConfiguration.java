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

import com.alibaba.cloud.ai.service.generator.GraphProjectContributor;
import com.alibaba.cloud.ai.service.generator.GraphProjectDescription;
import com.alibaba.cloud.ai.service.generator.ProjectGenerator;
import io.spring.initializr.generator.condition.ConditionalOnRequestedDependency;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.alibaba.cloud.ai.config.GraphProjectGenerationConfiguration.GRAPH_PROJECT_DEPENDENCY;

@Configuration(proxyBeanMethods = false)
@ConditionalOnRequestedDependency(GRAPH_PROJECT_DEPENDENCY)
public class GraphProjectGenerationConfiguration {

	public static final String GRAPH_PROJECT_DEPENDENCY = "spring-ai-alibaba-graph";

	private final GraphProjectDescription description;

	private final List<ProjectGenerator> projectGenerators;

	public GraphProjectGenerationConfiguration(GraphProjectDescription description,
			List<ProjectGenerator> projectGenerators) {
		this.description = description;
		this.projectGenerators = projectGenerators;
	}

	@Bean
	public GraphProjectContributor graphProjectContributor() {
		return new GraphProjectContributor(description, projectGenerators);
	}

}

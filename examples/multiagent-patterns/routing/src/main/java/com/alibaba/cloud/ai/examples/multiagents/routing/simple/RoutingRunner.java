/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.examples.multiagents.routing.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the routing demo when {@code routing.runner.enabled=true}: one query through
 * classify → parallel agents → synthesize.
 */
@Component
@ConditionalOnProperty(name = "routing.runner.enabled", havingValue = "true")
public class RoutingRunner implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(RoutingRunner.class);

	private final RouterService routerService;

	public RoutingRunner(RouterService routerService) {
		this.routerService = routerService;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		String query = "How do I authenticate API requests?";
		log.info("Query: {}", query);
		RouterService.RouterResult result = routerService.run(query);
		log.info("Classifications:");
		result.classifications().forEach(c -> log.info("  {}: {}", c.source(), c.query()));
		log.info("---");
		log.info("Final answer:\n{}", result.finalAnswer());
	}
}

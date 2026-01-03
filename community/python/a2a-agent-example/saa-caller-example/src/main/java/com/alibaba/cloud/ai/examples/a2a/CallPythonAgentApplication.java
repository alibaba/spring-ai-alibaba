/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.examples.a2a;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring AI Alibaba application that calls Python A2A agents.
 *
 * <p>This example demonstrates how to:
 * <ul>
 *   <li>Discover Python agents registered in Nacos</li>
 *   <li>Call them using the A2A protocol via A2aRemoteAgent</li>
 * </ul>
 */
@SpringBootApplication
public class CallPythonAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(CallPythonAgentApplication.class, args);
	}

}

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
package com.alibaba.cloud.ai.example.manus.dynamic.namespace.service;

import com.alibaba.cloud.ai.example.manus.dynamic.namespace.entity.NamespaceEntity;
import com.alibaba.cloud.ai.example.manus.dynamic.namespace.repository.NamespaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class NamespaceDataInitialization implements CommandLineRunner {

	private static final Logger log = LoggerFactory.getLogger(NamespaceDataInitialization.class);

	@Autowired
	private NamespaceRepository namespaceRepository;

	@Override
	public void run(String... args) throws Exception {
		initializeDefaultNamespaces();
	}

	private void initializeDefaultNamespaces() {
		try {
			// Check if default namespace already exists
			if (namespaceRepository.count() == 0) {
				NamespaceEntity defaultNamespace = new NamespaceEntity();
				defaultNamespace.setName("Default Namespace");
				defaultNamespace.setCode("default");
				defaultNamespace.setDescription("Default namespace for general purpose use");
				namespaceRepository.save(defaultNamespace);
				log.info("Default namespace initialized successfully");
			}
		}
		catch (Exception e) {
			log.error("Error initializing default namespaces: {}", e.getMessage());
		}
	}

}

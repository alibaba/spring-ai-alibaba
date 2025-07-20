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
import com.alibaba.cloud.ai.example.manus.dynamic.namespace.namespace.vo.NamespaceConfig;
import com.alibaba.cloud.ai.example.manus.dynamic.namespace.repository.NamespaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NamespaceServiceImpl implements NamespaceService {

	private static final Logger log = LoggerFactory.getLogger(NamespaceServiceImpl.class);

	private final NamespaceRepository repository;

	@Autowired
	public NamespaceServiceImpl(NamespaceRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<NamespaceConfig> getAllNamespaces() {
		return repository.findAll().stream().map(NamespaceEntity::mapToNamespaceConfig).collect(Collectors.toList());
	}

	@Override
	public NamespaceConfig getNamespaceById(String id) {
		NamespaceEntity entity = repository.findById(Long.parseLong(id))
			.orElseThrow(() -> new IllegalArgumentException("Namespace not found: " + id));
		return entity.mapToNamespaceConfig();
	}

	@Override
	public NamespaceConfig createNamespace(NamespaceConfig config) {
		try {
			// Check if a Namespace with the same name already exists
			NamespaceEntity existingNamespace = repository.findByName(config.getName());
			if (existingNamespace != null) {
				log.info("Found Namespace with same name: {}, updating Namespace", config.getName());
				config.setId(existingNamespace.getId());
				return updateNamespace(config);
			}

			// Check if a Namespace with the same code already exists
			NamespaceEntity existingNamespaceByCode = repository.findByCode(config.getCode());
			if (existingNamespaceByCode != null) {
				log.info("Found Namespace with same code: {}, updating Namespace", config.getCode());
				config.setId(existingNamespaceByCode.getId());
				return updateNamespace(config);
			}

			NamespaceEntity entity = new NamespaceEntity();
			updateEntityFromConfig(entity, config);
			entity = repository.save(entity);
			log.info("Successfully created new Namespace: {}", config.getName());
			return entity.mapToNamespaceConfig();
		}
		catch (Exception e) {
			log.warn("Exception occurred during Namespace creation: {}, error message: {}", config.getName(),
					e.getMessage());
			// If it's a uniqueness constraint violation exception, try returning the
			// existing Namespace
			if (e.getMessage() != null && e.getMessage().contains("Unique")) {
				NamespaceEntity existingNamespace = repository.findByName(config.getName());
				if (existingNamespace != null) {
					log.info("Return existing Namespace: {}", config.getName());
					return existingNamespace.mapToNamespaceConfig();
				}
			}
			throw e;
		}
	}

	@Override
	public NamespaceConfig updateNamespace(NamespaceConfig config) {
		NamespaceEntity entity = repository.findById(config.getId())
			.orElseThrow(() -> new IllegalArgumentException("Namespace not found: " + config.getId()));
		updateEntityFromConfig(entity, config);
		entity = repository.save(entity);
		return entity.mapToNamespaceConfig();
	}

	@Override
	public void deleteNamespace(String id) {
		repository.deleteById(Long.parseLong(id));
	}

	private void updateEntityFromConfig(NamespaceEntity entity, NamespaceConfig config) {
		entity.setName(config.getName());
		entity.setCode(config.getCode());
		entity.setDescription(config.getDescription());
	}

}

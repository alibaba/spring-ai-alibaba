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
package com.alibaba.cloud.ai.manus.prompt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.alibaba.cloud.ai.manus.prompt.model.po.PromptEntity;

import java.util.List;

@Repository
public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

	PromptEntity findByNamespaceAndPromptName(String namespace, String promptName);

	PromptEntity findByPromptName(String promptName);

	List<PromptEntity> getAllByNamespace(String namespace);

	@Query("SELECT e FROM PromptEntity e WHERE e.namespace = :namespace OR e.namespace IS NULL OR e.namespace = ''")
	List<PromptEntity> findByPromptNameWithDefault(String namespace);

}

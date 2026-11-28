/*
 * Copyright 2025-2026 the original author or authors.
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
package com.alibaba.cloud.ai.graph.agent.memory;

import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.postgresql.ds.PGSimpleDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static com.alibaba.cloud.ai.graph.agent.memory.PreferenceMemory.Key.REPLY_FORMAT;

@Testcontainers(disabledWithoutDocker = true)
class PreferenceMemoryPostgreSqlTest {

	@Container
	private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

	@Test
	void comparesVersionsAndPersistsReceiptsAcrossIndependentPostgreSqlConnections() throws Exception {
		PreferenceMemoryTest.verifyConcurrentCorrections(dataSource(), dataSource());
	}

	@Test
	void keepsTheDeletionWhenAnApprovedSaveIsRetriedAfterCheckpointFailure(@TempDir Path checkpoints) throws Exception {
		PreferenceMemoryTest.verifyApprovedReceiptAfterCheckpointFailure(dataSource(), dataSource(), checkpoints,
				REPLY_FORMAT, "markdown", Optional.empty());
	}

	private static PGSimpleDataSource dataSource() {
		var source = new PGSimpleDataSource();
		source.setURL(POSTGRES.getJdbcUrl());
		source.setUser(POSTGRES.getUsername());
		source.setPassword(POSTGRES.getPassword());
		return source;
	}

}

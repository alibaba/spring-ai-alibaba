/*
 * Copyright 2024-2026 the original author or authors.
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
package com.alibaba.cloud.ai.examples.documentation.framework.advanced;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.util.JsonHelper;

import static org.assertj.core.api.Assertions.assertThat;

class ContextManagementRecipesTest {

	@Test
	void appliesDistinctInputBudgetsToThePrimaryAndFallback() throws Exception {
		var routed = ContextManagementRecipes.routeWithCapacity();
		assertThat(routed.answer()).isEqualTo("wide model answered");
		assertThat(routed.primaryCalls()).isZero();
	}

	@Test
	void restoresTheCheckpointAndReadsOnlyTheAuthorizedArchive(@TempDir Path directory) throws Exception {
		var archived = ContextManagementRecipes.archiveAcrossRuns(directory);
		assertThat(archived.firstPage()).contains("Inventory page evidence.");
		assertThat(archived.nextPage()).isEqualTo(2);
		assertThat(archived.summaryCalls()).isEqualTo(1);
		assertThat(archived.summaryRestored()).isTrue();
		assertThat(archived.foreignReadRejected()).isTrue();
	}

	@Test
	void usesTheDelegatedCaseAndTenantForOnDemandContext(@TempDir Path directory) throws Exception {
		var delegated = ContextManagementRecipes.delegateWithOnDemandContext(directory);
		var output = new JsonHelper().fromJsonToMap(delegated.answer());
		assertThat(output).containsEntry("answer", "Inventory reviewed for warehouse-a")
			.containsEntry("outputs", Map.of("review", "Inventory reviewed for warehouse-a"));
		assertThat(delegated.factoriesCalled()).isEqualTo(1);
		assertThat(delegated.childSawOnlySelectedData()).isTrue();
	}

}

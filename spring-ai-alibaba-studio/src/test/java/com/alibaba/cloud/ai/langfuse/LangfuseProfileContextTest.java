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

package com.alibaba.cloud.ai.langfuse;

import com.alibaba.cloud.ai.StudioApplication;
import com.alibaba.cloud.ai.graph.observation.GraphObservationLifecycleListener;

import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = StudioApplication.class)
@ActiveProfiles({ "graph", "langfuse" })
class LangfuseProfileContextTest {

	@Autowired
	private ObservationRegistry observationRegistry;

	@Autowired
	private GraphObservationLifecycleListener graphObservationLifecycleListener;

	@Test
	void contextLoadsWithLangfuseProfile() {
		assertThat(observationRegistry).isNotNull();
		assertThat(graphObservationLifecycleListener).isNotNull();
	}

}

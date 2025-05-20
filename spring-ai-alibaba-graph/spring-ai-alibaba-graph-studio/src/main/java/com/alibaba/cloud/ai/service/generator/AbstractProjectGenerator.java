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
package com.alibaba.cloud.ai.service.generator;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;

import java.nio.file.Path;

public abstract class AbstractProjectGenerator implements ProjectGenerator {

	protected DSLAdapter dslAdapter;

	public AbstractProjectGenerator(DSLAdapter dslAdapter) {
		this.dslAdapter = dslAdapter;
	}

	@Override
	public void generate(GraphProjectDescription projectDescription, Path projectRoot) {
		// 1. parse dsl using dslAdapter
		App app = dslAdapter.importDSL(projectDescription.getDsl());

	}

	protected abstract void doGenerate(App app, GraphProjectDescription projectDescription, Path projectRoot);

}

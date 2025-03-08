/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.DSLAPI;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import com.alibaba.cloud.ai.service.dsl.DSLDialectType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("graph-studio/api/dsl")
public class DSLController implements DSLAPI {

	private final List<DSLAdapter> adapters;

	private final AppSaver appSaver;

	public DSLController(AppSaver appSaver, List<DSLAdapter> adapters) {
		this.appSaver = appSaver;
		this.adapters = adapters;
	}

	@Override
	public DSLAdapter getAdapter(DSLDialectType dialect) {
		return adapters.stream().filter(adapter -> adapter.supportDialect(dialect)).findFirst().orElse(null);
	}

	@Override
	public AppSaver getAppSaver() {
		return appSaver;
	}

}

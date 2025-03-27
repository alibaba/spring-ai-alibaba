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
package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.saver.AppSaver;

/**
 * DSLAdapter defined the mutual conversion between specific DSL(e.g.) and {@link App}
 * model.
 */
public interface DSLAdapter {

	/**
	 * Turn app into DSL
	 * @param app {@link App}
	 * @return the specific dialect DSL
	 */
	String exportDSL(App app);

	/**
	 * Turn DSL into app
	 * @param dsl a specific formatted string
	 * @return unified app model {@link AppSaver}
	 */
	App importDSL(String dsl);

	/**
	 * Judge if current implementation supports this dialect
	 * @param dialectType a specific dsl format, see {@link DSLDialectType}
	 * @return if supports return true, otherwise return false
	 */
	Boolean supportDialect(DSLDialectType dialectType);

}

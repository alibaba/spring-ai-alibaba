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

package com.alibaba.cloud.ai.model;

import org.springframework.ai.model.Model;

/**
 * Title rerank model interface.<br>
 * Description Rerank model is used to calculate the semantic match between the list of
 * candidate documents and the user query .<br>
 *
 * @author yuanci.ytb
 * @since 1.0.0-M2
 */

public interface RerankModel extends Model<RerankRequest, RerankResponse> {

	@Override
	RerankResponse call(RerankRequest request);

}

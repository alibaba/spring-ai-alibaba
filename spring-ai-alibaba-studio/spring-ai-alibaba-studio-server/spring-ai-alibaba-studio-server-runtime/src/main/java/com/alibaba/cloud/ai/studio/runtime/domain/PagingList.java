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

package com.alibaba.cloud.ai.studio.runtime.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * A generic class for handling paginated data lists.
 *
 * @since 1.0.0.3
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PagingList<T> implements Serializable {

	/** Current page number */
	private Integer current = 0;

	/** Number of items per page */
	private Integer size = 0;

	/** Total number of items */
	private Long total = 0L;

	/** List of items for the current page */
	private List<T> records;

}

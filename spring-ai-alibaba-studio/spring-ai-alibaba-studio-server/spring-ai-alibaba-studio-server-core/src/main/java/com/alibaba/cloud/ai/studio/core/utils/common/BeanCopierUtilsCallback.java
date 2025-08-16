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

package com.alibaba.cloud.ai.studio.core.utils.common;

/**
 * A callback interface for bean copying operations. Provides a mechanism to customize the
 * copying process between source and target objects.
 *
 * @since 1.0.0.3
 */

public interface BeanCopierUtilsCallback<S, T> {

	/**
	 * Callback method executed during the bean copying process.
	 * @param s source object
	 * @param t target object
	 */
	void callback(S s, T t);

}

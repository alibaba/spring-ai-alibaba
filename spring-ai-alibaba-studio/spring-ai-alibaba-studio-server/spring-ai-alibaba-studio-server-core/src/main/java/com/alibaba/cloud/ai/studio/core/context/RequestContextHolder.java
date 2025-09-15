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

package com.alibaba.cloud.ai.studio.core.context;

import com.alibaba.cloud.ai.studio.runtime.domain.RequestContext;

/**
 * A holder class for managing request context in a thread-local manner. Provides methods
 * to set, get and clear the request context for the current thread.
 *
 * @since 1.0.0.3
 */
public class RequestContextHolder {

	/**
	 * ThreadLocal variable to store request context for each thread
	 */
	private static final ThreadLocal<RequestContext> requestContextThreadLocal = new ThreadLocal<>();

	/**
	 * Sets the request context for the current thread
	 * @param requestContext the context to be set
	 */
	public static void setRequestContext(RequestContext requestContext) {
		requestContextThreadLocal.set(requestContext);
	}

	/**
	 * Gets the request context for the current thread
	 * @return the current thread's request context
	 */
	public static RequestContext getRequestContext() {
		return requestContextThreadLocal.get();
	}

	/**
	 * Clears the request context for the current thread
	 */
	public static void clearRequestContext() {
		requestContextThreadLocal.remove();
	}

}

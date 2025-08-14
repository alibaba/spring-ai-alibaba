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

package com.alibaba.cloud.ai.studio.core.base.mq;

/**
 * Interface for handling message sending callbacks. Provides methods to handle successful
 * message delivery and error cases.
 *
 * @since 1.0.0.3
 */
public interface SendCallback {

	/**
	 * Called when message is successfully sent.
	 * @param sendResult Result of the send operation
	 */
	void onSuccess(final SendResult sendResult);

	/**
	 * Called when an error occurs during message sending.
	 * @param e The exception that occurred
	 */
	void onError(final Throwable e);

}

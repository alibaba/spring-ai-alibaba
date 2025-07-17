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
package com.alibaba.cloud.ai.example.manus.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JmanusEventPublisher<T extends JmanusEvent> {

	private static final Logger logger = LoggerFactory.getLogger(JmanusEventPublisher.class);

	// 监听器无法动态注册，无需线程安全
	private Map<Class<T>, List<JmanusListener<T>>> listeners = new HashMap<>();

	public void publish(T event) {
		List<JmanusListener<T>> jmanusListeners = listeners.get(event.getClass());
		for (JmanusListener<T> jmanusListener : jmanusListeners) {
			try {
				jmanusListener.onEvent(event);
			}
			catch (Exception e) {
				// 这里忽略异常，避免影响其他监听器的执行
				logger.error("Error occurred while processing event: {}", e.getMessage());
			}
		}
	}

	void registerListener(Class<T> eventClass, JmanusListener<T> listener) {
		List<JmanusListener<T>> jmanusListeners = listeners.get(eventClass);
		if (jmanusListeners == null) {
			listeners.put(eventClass, List.of(listener));
		}
		else {
			jmanusListeners.add(listener);
		}
	}

}

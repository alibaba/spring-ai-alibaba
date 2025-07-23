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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JmanusEventPublisher {

	private static final Logger logger = LoggerFactory.getLogger(JmanusEventPublisher.class);

	// 监听器无法动态注册，无需线程安全
	private Map<Class<? extends JmanusEvent>, List<JmanusListener<? super JmanusEvent>>> listeners = new HashMap<>();

	public void publish(JmanusEvent event) {
		Class<? extends JmanusEvent> eventClass = event.getClass();
		for (Map.Entry<Class<? extends JmanusEvent>, List<JmanusListener<? super JmanusEvent>>> entry : listeners
			.entrySet()) {
			// 这里父类也可以通知
			if (entry.getKey().isAssignableFrom(eventClass)) {
				for (JmanusListener<? super JmanusEvent> listener : entry.getValue()) {
					try {
						listener.onEvent(event);
					}
					catch (Exception e) {
						logger.error("Error occurred while processing event: {}", e.getMessage(), e);
					}
				}
			}
		}
	}

	void registerListener(Class<? extends JmanusEvent> eventClass, JmanusListener<? super JmanusEvent> listener) {
		List<JmanusListener<? super JmanusEvent>> jmanusListeners = listeners.get(eventClass);
		if (jmanusListeners == null) {
			List<JmanusListener<? super JmanusEvent>> list = new ArrayList<>();
			list.add(listener);
			listeners.put(eventClass, list);
		}
		else {
			jmanusListeners.add(listener);
		}
	}

}

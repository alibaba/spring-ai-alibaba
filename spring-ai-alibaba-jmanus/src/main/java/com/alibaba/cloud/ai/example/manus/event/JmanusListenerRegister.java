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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;

/**
 * @author dahua
 * @time 2025/7/15
 * @desc jmanus 事件监听器注册
 */
@Component
public class JmanusListenerRegister implements BeanPostProcessor {

	@Autowired
	private JmanusEventPublisher jmanusEventPublisher;

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof JmanusListener) {
			ResolvableType resolvableType = ResolvableType.forClass(bean.getClass()).as(JmanusListener.class);
			ResolvableType eventType = resolvableType.getGeneric(0);
			Class<?> eventClass = eventType.resolve();
			Class<? extends JmanusEvent> jmanusEventClass;
			try {
				jmanusEventClass = (Class<? extends JmanusEvent>) eventClass;
			}
			catch (Exception e) {
				throw new IllegalArgumentException("The listener can only listen to JmanusEvent type");
			}
			jmanusEventPublisher.registerListener(jmanusEventClass, (JmanusListener) bean);
		}
		return bean;
	}

}

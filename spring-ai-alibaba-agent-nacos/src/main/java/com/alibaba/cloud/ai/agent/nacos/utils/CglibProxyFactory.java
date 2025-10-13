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

package com.alibaba.cloud.ai.agent.nacos.utils;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.alibaba.cloud.ai.observation.model.ObservationMetadataAwareOptions;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.springframework.ai.chat.prompt.ChatOptions;

/**
 * Dynamic proxy factory based on CGLIB
 * Implements multi-interface functionality by creating subclasses
 */
public class CglibProxyFactory {

	/**
	 * Create a proxy object that implements both ChatOptions and ObservationMetadataAwareOptions interfaces
	 *
	 * @param chatOptions Original ChatOptions object
	 * @param initialMetadata Initial observation metadata
	 * @return Proxy object that implements both ChatOptions and ObservationMetadataAwareOptions interfaces
	 */
	public static Object createProxy(ChatOptions chatOptions, Map<String, String> initialMetadata) {
		// Create CGLIB enhancer
		Enhancer enhancer = new Enhancer();

		// Set parent class to ChatOptionsImpl
		enhancer.setSuperclass(chatOptions.getClass());

		// Set interfaces to implement
		enhancer.setInterfaces(new Class[] {ChatOptions.class, ObservationMetadataAwareOptions.class});

		// Set callback handler
		enhancer.setCallback(new CglibMethodInterceptor(chatOptions, initialMetadata));

		// Create proxy object
		return enhancer.create();
	}

	/**
	 * CGLIB method interceptor
	 */
	private static class CglibMethodInterceptor implements MethodInterceptor {

		private final ChatOptions chatOptions;
		private final Map<String, String> observationMetadata;
		private String observationName;
		private Boolean observationEnabled;

		public CglibMethodInterceptor(ChatOptions chatOptions, Map<String, String> initialMetadata) {
			this.chatOptions = chatOptions;
			this.observationMetadata = new HashMap<>(initialMetadata);
			this.observationName = "ChatObservation";
			this.observationEnabled = true;
		}

		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			String methodName = method.getName();
			Class<?> declaringClass = method.getDeclaringClass();

			// 1. Intercept copy()
			if (isCopyMethod(method)) {
				return createCopiedProxy();
			}

			// Handle ChatOptions interface methods - forward directly to original object
			if (declaringClass == ChatOptions.class) {
				return method.invoke(chatOptions, args);
			}

			// Handle ObservationMetadataAwareOptions interface methods
			if (declaringClass == ObservationMetadataAwareOptions.class) {
				return handleObservationMethod(methodName, args);
			}

			// Handle Object class methods
			if (declaringClass == Object.class) {
				return handleObjectMethod(methodName, args, obj);
			}

			// Handle parent class methods - forward to original object
			return method.invoke(chatOptions, args);
		}

		private boolean isCopyMethod(Method method) {
			return "copy".equals(method.getName())
					&& method.getParameterCount() == 0
					&& ChatOptions.class.isAssignableFrom(method.getReturnType());
		}

		private Object createCopiedProxy() {
			ChatOptions copiedChatOptions;
			try {
				Method copyMethod = chatOptions.getClass().getMethod("copy");
				// If it's private or protected, need to set setAccessible(true)
				copyMethod.setAccessible(true);
				Object result = copyMethod.invoke(chatOptions);
				if (!(result instanceof ChatOptions)) {
					throw new IllegalStateException("copy() method did not return a ChatOptions instance");
				}
				copiedChatOptions = (ChatOptions) result;
			}
			catch (NoSuchMethodException e) {
				throw new IllegalStateException("ChatOptions implementation missing copy() method", e);
			}
			catch (Exception e) {
				throw new RuntimeException("Failed to invoke copy() method", e);
			}

			// Create new proxy object (deep copy metadata)
			return CglibProxyFactory.createProxy(
					copiedChatOptions,
					new HashMap<>(this.observationMetadata)
			);
		}

		/**
		 * Handle observation methods - dynamic processing based on method name
		 */
		private Object handleObservationMethod(String methodName, Object[] args) {
			switch (methodName) {
			case "getObservationMetadata":
				return observationMetadata;

			case "setObservationMetadata":
				if (args != null && args.length > 0 && args[0] instanceof Map) {
					observationMetadata.clear();
					observationMetadata.putAll((Map<String, String>) args[0]);
				}
				return null;

			case "addObservationMetadata":
				if (args != null && args.length >= 2) {
					observationMetadata.put((String) args[0], (String) args[1]);
				}
				return null;

			case "getObservationName":
				return observationName;

			case "setObservationName":
				if (args != null && args.length > 0) {
					observationName = (String) args[0];
				}
				return null;

			case "isObservationEnabled":
				return observationEnabled;

			case "setObservationEnabled":
				if (args != null && args.length > 0) {
					observationEnabled = (Boolean) args[0];
				}
				return null;

			default:
				throw new UnsupportedOperationException("Unknown observation method: " + methodName);
			}
		}

		/**
		 * Handle Object class methods
		 */
		private Object handleObjectMethod(String methodName, Object[] args, Object obj) {
			switch (methodName) {
			case "toString":
				return "CglibProxy{" +
						"chatOptions=" + chatOptions +
						", observationMetadata=" + observationMetadata +
						", observationName='" + observationName + '\'' +
						", observationEnabled=" + observationEnabled +
						'}';

			case "equals":
				if (args != null && args.length > 0) {
					return obj == args[0];
				}
				return false;

			case "hashCode":
				return System.identityHashCode(obj);

			default:
				throw new UnsupportedOperationException("Unknown Object method: " + methodName);
			}
		}
	}
}

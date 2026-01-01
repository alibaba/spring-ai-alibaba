/*
 * Copyright 2024-2026 the original author or authors.
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
 * 基于CGLIB的动态代理工�?
 * 通过创建子类的方式实现多接口功能
 */
public class ChatOptionsProxy {

	/**
	 * 创建同时实现ChatOptions和ObservationMetadataAwareOptions接口的代理对�?
	 *
	 * @param chatOptions 原始的ChatOptions对象
	 * @param initialMetadata 初始的观察元数据
	 * @return 代理对象，同时实现了ChatOptions和ObservationMetadataAwareOptions接口
	 */
	public static Object createProxy(ChatOptions chatOptions, Map<String, String> initialMetadata) {
		// 创建CGLIB增强�?
		Enhancer enhancer = new Enhancer();

		// 设置父类为ChatOptionsImpl
		enhancer.setSuperclass(chatOptions.getClass());

		// 设置要实现的接口
		enhancer.setInterfaces(new Class[] {ChatOptions.class, ObservationMetadataAwareOptions.class});

		// 设置回调处理�?
		enhancer.setCallback(new CglibMethodInterceptor(chatOptions, initialMetadata));

		// 创建代理对象
		return enhancer.create();
	}

	/**
	 * CGLIB方法拦截�?
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

			// 1. 拦截 copy()
			if (isCopyMethod(method)) {
				return createCopiedProxy();
			}

			// 处理ChatOptions接口的方�?- 直接转发到原始对�?
			if (declaringClass == ChatOptions.class) {
				return method.invoke(chatOptions, args);
			}

			// 处理ObservationMetadataAwareOptions接口的方�?
			if (declaringClass == ObservationMetadataAwareOptions.class) {
				return handleObservationMethod(methodName, args);
			}

			// 处理Object类的方法
			if (declaringClass == Object.class) {
				return handleObjectMethod(methodName, args, obj);
			}

			// 处理父类方法 - 转发到原始对�?
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
				// 如果�?private �?protected，需�?setAccessible(true)
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

			// 创建新的代理对象（深拷贝 metadata�?
			return ChatOptionsProxy.createProxy(
					copiedChatOptions,
					new HashMap<>(this.observationMetadata)
			);
		}

		/**
		 * 处理观察方法 - 基于方法名动态处�?
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
		 * 处理Object类的方法
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

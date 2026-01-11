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

package com.alibaba.cloud.ai.agent.nacos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.alibaba.cloud.ai.agent.nacos.vo.ModelVO;
import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.exception.NacosException;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;

public class NacosModelInjector {


	public static ModelVO getModelByAgentName(NacosOptions nacosOptions) {
		try {
			String dataIdT = (nacosOptions.isModelEncrypted() ? "cipher-kms-aes-256-" : "") + "model.json";
			String config = nacosOptions.getNacosConfigService()
					.getConfig(dataIdT, "ai-agent-" + nacosOptions.getAgentName(), 3000L);
			return JSON.parseObject(config, ModelVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}


	public static void replaceModel(ChatClient chatClient, ChatModel chatModel, OpenAiChatOptions openAiChatOptions) throws Exception {
		Object defaultChatClientRequest = getField(chatClient, "defaultChatClientRequest");
		modifyFinalField(defaultChatClientRequest, "chatModel", chatModel);
		modifyFinalField(defaultChatClientRequest, "chatOptions", openAiChatOptions);
	}

	private static Object getField(Object obj, String fieldName) throws Exception {
		Field field = obj.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		return field.get(obj);
	}

	public static void modifyFinalField(Object targetObject, String fieldName, Object newValue) throws Exception {
		Field field = targetObject.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);

		try {
			// Java 8及以下版本的方式
			Field modifiersField = Field.class.getDeclaredField("modifiers");
			modifiersField.setAccessible(true);
			modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
			field.set(targetObject, newValue);
		}
		catch (NoSuchFieldException e) {
			// Java 9及以上版本的方式
			try {
				// 使用反射修改final字段
				Field[] fields = field.getClass().getDeclaredFields();
				for (Field f : fields) {
					if ("modifiers".equals(f.getName())) {
						f.setAccessible(true);
						f.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
						break;
					}
				}
				field.set(targetObject, newValue);
			}
			catch (Exception ex) {
				// 如果上述方式都不行，尝试使用Unsafe（不推荐但有时有效）
				modifyFinalFieldWithUnsafe(field, targetObject, newValue);
			}
		}
	}

	/**
	 * 使用Unsafe修改final字段（适用于Java 12+）
	 */
	private static void modifyFinalFieldWithUnsafe(Field field, Object targetObject, Object newValue) throws Exception {
		try {
			Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
			Field unsafeInstanceField = unsafeClass.getDeclaredField("theUnsafe");
			unsafeInstanceField.setAccessible(true);
			Object unsafeInstance = unsafeInstanceField.get(null);

			Method putObjectMethod = unsafeClass.getMethod("putObject", Object.class, long.class, Object.class);
			Method staticFieldOffsetMethod = unsafeClass.getMethod("staticFieldOffset", Field.class);

			long offset = (Long) staticFieldOffsetMethod.invoke(unsafeInstance, field);
			putObjectMethod.invoke(unsafeInstance, targetObject, offset, newValue);
		}
		catch (Exception e) {
			throw new RuntimeException("无法修改final字段: " + field.getName(), e);
		}
	}
}

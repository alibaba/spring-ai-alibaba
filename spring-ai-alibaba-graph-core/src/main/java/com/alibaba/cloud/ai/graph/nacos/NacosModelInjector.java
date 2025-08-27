package com.alibaba.cloud.ai.graph.nacos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.api.config.listener.AbstractListener;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.client.config.NacosConfigService;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.SimpleApiKey;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;

public class NacosModelInjector {


	public static ModelVO getModelByAgentId(NacosConfigService nacosConfigService, String agentId) {
		try {
			String config = nacosConfigService.getConfig(String.format("model-%s.json", agentId), "nacos-ai-agent", 3000L);
			return JSON.parseObject(config, ModelVO.class);
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static void injectModel(ChatModel chatModel, NacosConfigService nacosConfigService, String agentId) {
		try {
			nacosConfigService.addListener(String.format("model-%s.json", agentId), "nacos-ai-agent", new AbstractListener() {
				@Override
				public void receiveConfigInfo(String configInfo) {
					ModelVO modelVO = JSON.parseObject(configInfo, ModelVO.class);
					try {
						replaceModel(chatModel, modelVO);
					}
					catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
		catch (NacosException e) {
			throw new RuntimeException(e);
		}
	}

	public static void replaceModel(ChatModel chatModel, ModelVO modelVO) throws Exception {
		Field openAiChatOptionsField = chatModel.getClass().getDeclaredField("defaultOptions");
		openAiChatOptionsField.setAccessible(true);
		OpenAiChatOptions openAiChatOptions = (OpenAiChatOptions) openAiChatOptionsField.get(chatModel);
		openAiChatOptions.setModel(modelVO.getModel());
		if (modelVO.getTemperature() != null) {
			openAiChatOptions.setTemperature(Double.parseDouble(modelVO.getTemperature()));
		}
		if (modelVO.getMaxTokens() != null) {
			openAiChatOptions.setMaxTokens(Integer.parseInt(modelVO.getMaxTokens()));
		}
		Field openAiApiField = chatModel.getClass().getDeclaredField("openAiApi");
		openAiApiField.setAccessible(true);

		OpenAiApi openAiApi = (OpenAiApi) openAiApiField.get(chatModel);
		// 修改baseUrl字段

		modifyFinalField(openAiApi, "baseUrl", modelVO.getBaseUrl());
		// 修改apiKey字段
		SimpleApiKey simpleApiKey = new SimpleApiKey(modelVO.getApiKey());
		modifyFinalField(openAiApi, "apiKey", simpleApiKey);
		System.out.println(openAiApi);
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

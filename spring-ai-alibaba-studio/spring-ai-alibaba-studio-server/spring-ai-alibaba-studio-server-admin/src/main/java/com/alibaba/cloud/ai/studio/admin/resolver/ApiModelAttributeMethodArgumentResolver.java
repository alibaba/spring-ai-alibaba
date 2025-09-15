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

package com.alibaba.cloud.ai.studio.admin.resolver;

import com.alibaba.cloud.ai.studio.runtime.utils.JsonUtils;
import com.alibaba.cloud.ai.studio.admin.annotation.ApiModelAttribute;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.HashMap;
import java.util.Map;

/**
 * Resolver for handling API model attributes in method arguments. Converts request
 * parameters to annotated model objects.
 *
 * @since 1.0.0.3
 */
public class ApiModelAttributeMethodArgumentResolver implements HandlerMethodArgumentResolver {

	/**
	 * Checks if the parameter is supported by this resolver.
	 * @param parameter The method parameter to check
	 * @return true if parameter has ApiModelAttribute annotation and is not a simple
	 * property
	 */
	@Override
	public boolean supportsParameter(@NotNull MethodParameter parameter) {
		return (parameter.hasParameterAnnotation(ApiModelAttribute.class)
				&& !BeanUtils.isSimpleProperty(parameter.getParameterType()));
	}

	/**
	 * Resolves the method argument by converting request parameters to the target object.
	 * @param parameter The method parameter to resolve
	 * @param mavContainer The model and view container
	 * @param webRequest The current web request
	 * @param binderFactory The data binder factory
	 * @return The resolved argument object
	 */
	@Override
	public Object resolveArgument(@NotNull MethodParameter parameter, ModelAndViewContainer mavContainer,
			@NotNull NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		Map<String, String[]> parameterMap = webRequest.getParameterMap();

		Map<String, Object> processedParams = new HashMap<>();
		for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
			String[] values = entry.getValue();
			if (values.length == 1) {
				processedParams.put(entry.getKey(), values[0]);
			}
			else {
				processedParams.put(entry.getKey(), values);
			}
		}
		String json = JsonUtils.toJson(processedParams);

		return JsonUtils.fromJson(json, parameter.getParameterType());
	}

}

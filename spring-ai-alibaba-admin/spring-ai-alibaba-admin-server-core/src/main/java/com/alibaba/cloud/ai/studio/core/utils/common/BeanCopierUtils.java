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

package com.alibaba.cloud.ai.studio.core.utils.common;

import org.springframework.cglib.beans.BeanCopier;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for copying properties between Java beans using CGLIB BeanCopier.
 * Provides efficient and type-safe bean property copying operations.
 *
 * @since 1.0.0.3
 */

public class BeanCopierUtils {

	/** Cache for storing BeanCopier instances to improve performance */
	private static final Map<String, BeanCopier> beanCopierMap = new HashMap<>();

	/**
	 * Copies properties from source object to destination object.
	 * @param src Source object
	 * @param dest Destination object
	 */
	public static void copy(Object src, Object dest) {
		Objects.requireNonNull(src);
		Objects.requireNonNull(dest);
		String key = getKey(src, dest);
		BeanCopier beanCopier;
		if (!beanCopierMap.containsKey(key)) {
			beanCopier = BeanCopier.create(src.getClass(), dest.getClass(), false);
			beanCopierMap.put(key, beanCopier);
		}
		else {
			beanCopier = beanCopierMap.get(key);
		}
		beanCopier.copy(src, dest, null);
	}

	/**
	 * Creates a new instance of destination class and copies properties from source
	 * object.
	 * @param src Source object
	 * @param destClass Destination class
	 * @return New instance of destination class with copied properties
	 */
	public static <T> T copy(Object src, Class<T> destClass) {
		Objects.requireNonNull(src);
		Objects.requireNonNull(destClass);
		T dest;
		try {
			dest = destClass.newInstance();
		}
		catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalArgumentException(e);
		}
		copy(src, dest);
		return dest;
	}

	/**
	 * Copies a list of source objects to a list of destination objects.
	 * @param srcList Source object list
	 * @param destClass Destination class
	 * @return List of new destination objects
	 */
	public static <T, S> List<T> copyList(List<S> srcList, Class<T> destClass) {
		if (CollectionUtils.isEmpty(srcList)) {
			return new ArrayList<>(0);
		}

		Objects.requireNonNull(destClass);
		return srcList.stream().map(src -> copy(src, destClass)).collect(Collectors.toList());
	}

	/**
	 * Copies a list of source objects to a list of destination objects with custom
	 * callback.
	 * @param srcList Source object list
	 * @param destClass Destination class
	 * @param callback Custom callback for additional processing
	 * @return List of new destination objects
	 */
	public static <T, S> List<T> copyList(List<S> srcList, Class<T> destClass, BeanCopierUtilsCallback<S, T> callback) {
		if (CollectionUtils.isEmpty(srcList)) {
			return new ArrayList<>(0);
		}

		Objects.requireNonNull(destClass);
		return srcList.stream().map(src -> {
			T target = copy(src, destClass);
			if (callback != null) {
				callback.callback(src, target);
			}
			return target;
		}).collect(Collectors.toList());
	}

	/**
	 * Generates a unique key for BeanCopier cache based on source and destination
	 * classes.
	 */
	private static String getKey(Object src, Object dest) {
		return src.getClass().getName() + dest.getClass().getName();
	}

}

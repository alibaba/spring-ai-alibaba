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
package com.alibaba.cloud.ai.utils;

import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SpringApplicationUtil implements ApplicationContextAware, EnvironmentAware {

	private static ApplicationContext applicationContext;

	private static Environment environment;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		SpringApplicationUtil.applicationContext = applicationContext;
	}

	@Override
	public void setEnvironment(Environment environment) {
		SpringApplicationUtil.environment = environment;
	}

	/**
	 * get bean instance from spring container according to bean type
	 * @param clazz bean type
	 * @param <T> bean instance of specified type
	 * @return bean instance of specified type
	 */
	public static <T> T getBean(Class<T> clazz) {
		return applicationContext.getBean(clazz);
	}

	/**
	 * get bean instance from spring container based on bean name and bean type
	 * @param name bean name
	 * @param clazz bean type
	 * @param <T> bean instance of specified type
	 * @return bean instance of specified type
	 */
	public static <T> T getBean(String name, Class<T> clazz) {
		return applicationContext.getBean(name, clazz);
	}

	/**
	 * get bean instance from spring container based on bean name
	 * @param beanName bean name
	 * @return Object
	 */
	public static Object getBean(String beanName) {
		return applicationContext.getBean(beanName);
	}

	public static <T> Map<String, T> getBeans(Class<T> clazz) {
		return applicationContext.getBeansOfType(clazz);
	}

	/**
	 * determine whether there is a bean instance with the specified bean name in the
	 * spring container
	 * @param name bean name
	 * @return boolean
	 */
	public static boolean containsBean(String name) {
		return applicationContext.containsBean(name);
	}

	/**
	 * Get the type of instance corresponding to the bean name from the spring container
	 * based on the bean name
	 * @param name bean name
	 * @throws NoSuchBeanDefinitionException if no bean definition is found
	 */
	public static Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return applicationContext.getType(name);
	}

	/**
	 * Get the value of the corresponding configuration item based on the name of the
	 * configuration item key For example, there is a configuration of url=www.baidu.com
	 * in the application.properties configuration file. Then the value of
	 * getProperty("url") is www.baidu.com
	 * @param key The name of the configuration item, such as url
	 * @return Strings
	 */
	public static String getProperty(String key) {
		return environment.getProperty(key);
	}

	/**
	 * Get the value of the configuration item based on the name of the configuration item
	 * key and convert the value to the specified type For example, there is a
	 * configuration item age=18 in the application.properties configuration file. Because
	 * the value of age is an integer, we can directly obtain the age value of the integer
	 * through this method The result of getProperty("age",Integer.class) is the integer
	 * 18
	 * @param key The name of the configuration item
	 * @param targetType The type of value corresponding to the configuration item
	 * @return The value of the configuration item of the specified type
	 */
	public static <T> T getProperty(String key, Class<T> targetType) {
		return environment.getProperty(key, targetType);
	}

}

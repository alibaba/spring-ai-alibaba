/*
 * Copyright 2024 - 2024 the original author or authors.
 */

package com.alibaba.cloud.ai.example.manus.inhouse.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP 工具注解
 *
 * 用于标记需要自动注册为 MCP 工具的类
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpTool {

	/**
	 * 工具名称，为空时从工具对象的 getName() 方法获取
	 */
	String name() default "";

	/**
	 * 工具描述，为空时从工具对象的 getDescription() 方法获取
	 */
	String description() default "";

	/**
	 * 是否启用该工具
	 */
	boolean enabled() default true;

}
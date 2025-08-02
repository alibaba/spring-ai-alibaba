/*
 * Copyright 2024 - 2024 the original author or authors.
 */

package com.alibaba.cloud.ai.example.manus.inhouse.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * MCP 工具 Schema 注解
 *
 * 用于定义工具的 JSON Schema
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface McpToolSchema {

	/**
	 * JSON Schema 定义
	 */
	String value();

}
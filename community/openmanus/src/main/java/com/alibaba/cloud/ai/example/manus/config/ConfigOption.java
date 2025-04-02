package com.alibaba.cloud.ai.example.manus.config;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import java.lang.annotation.RetentionPolicy;

/**
 * 配置选项注解
 * <p>
 * 用于定义下拉框、单选框等的选项
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigOption {

	/**
	 * 选项值
	 */
	String value();

	/**
	 * 选项标签
	 * <p>
	 * 支持国际化key格式：config.option.{group}.{subGroup}.{key}.{value}
	 */
	String label() default "";

	/**
	 * 选项描述
	 * <p>
	 * 支持国际化key格式：config.option.desc.{group}.{subGroup}.{key}.{value}
	 */
	String description() default "";

	/**
	 * 选项图标（可选）
	 */
	String icon() default "";

	/**
	 * 选项是否禁用
	 */
	boolean disabled() default false;

}

package com.alibaba.cloud.ai.example.manus.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigInputType;

/**
 * 配置属性注解，支持三级配置结构：group.subgroup.key
 * <p>
 * 配置层级结构：
 * <ul>
 * <li>group: 顶层分组，如 browser, network, security 等</li>
 * <li>subGroup: 二级分组，如 browser.settings, browser.proxy 等</li>
 * <li>key: 具体配置项</li>
 * </ul>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperty {

	/**
	 * 顶层分组
	 */
	String group();

	/**
	 * 二级分组
	 */
	String subGroup();

	/**
	 * 配置项key
	 */
	String key();

	/**
	 * 配置项YAML完整路径，用来从yml里面找到特定的配置项作为默认值放到数据库里
	 */
	String path();

	/**
	 * 配置项描述
	 * <p>
	 * 支持国际化key格式：config.desc.{group}.{subGroup}.{key}
	 */
	String description() default "";

	/**
	 * 配置项默认值
	 */
	String defaultValue() default "";

	/**
	 * 配置项输入类型
	 * <p>
	 * 默认为文本输入框
	 */
	ConfigInputType inputType() default ConfigInputType.TEXT;

	/**
	 * 下拉框选项
	 * <p>
	 * 仅在 inputType = SELECT 时生效
	 */
	ConfigOption[] options() default {};

}

package com.alibaba.cloud.ai.example.manus.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.alibaba.cloud.ai.example.manus.config.entity.ConfigInputType;

/**
 * 配置属性注解，支持三级配置结构：group.subgroup.key
 * 
 * <p>
 * 配置层级结构：
 * <ul>
 * <li>group: 顶层分组，如 browser, network, security 等</li>
 * <li>subGroup: 二级分组，如 browser.settings, browser.proxy 等</li>
 * <li>key: 具体配置项</li>
 * </ul>
 * 
 * <p>
 * 使用示例：
 * 
 * <pre>
 * {@code
 * @ConfigProperty(group = "browser", // 顶层分组，用来在第一层级合并展示
 *         subGroup = "settings", // 二级分组，用来在第二层级合并展示
 *         key = "headless", // 配置项key
 *         path = "manus.browser.headless", // YAML完整路径，对应如果yml里面有配置，那配置是什么
 *         description = "是否启用浏览器无头模式", // 用来在配置项的上面展示配置信息
 *         defaultValue = "false")
 * private Boolean browserHeadless;
 * }
 * </pre>
 * 
 * <p>
 * 对应的国际化资源结构：
 * 
 * <pre>
 * config.group.browser=浏览器
 * config.group.browser.settings=基本设置
 * config.prop.browser.settings.headless=无头模式
 * config.desc.browser.settings.headless=是否启用浏览器无头模式
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigProperty {
    // ...existing properties...

    /**
     * 配置项的输入类型
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

/**
 * 配置选项注解
 * <p>
 * 用于定义下拉框、单选框等的选项
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
@interface ConfigOption {
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

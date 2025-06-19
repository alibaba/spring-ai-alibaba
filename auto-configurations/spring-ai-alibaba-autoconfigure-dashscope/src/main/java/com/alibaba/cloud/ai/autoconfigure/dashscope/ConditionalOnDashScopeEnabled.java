package com.alibaba.cloud.ai.autoconfigure.dashscope;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

/**
 * Provides a more succinct conditional {@systemProperty spring.ai.dashscope.enabled}
 *
 * @author yuhuangbin
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ConditionalOnProperty(value = "spring.ai.dashscope.enabled", matchIfMissing = true)
public @interface ConditionalOnDashScopeEnabled {

}

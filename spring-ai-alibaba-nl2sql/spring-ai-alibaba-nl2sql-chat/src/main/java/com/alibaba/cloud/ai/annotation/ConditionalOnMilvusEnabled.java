
package com.alibaba.cloud.ai.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ConditionalOnProperty(prefix = "spring.ai.alibaba.nl2sql.milvus", name = "enabled", havingValue = "true",
		matchIfMissing = false)
public @interface ConditionalOnMilvusEnabled {

}

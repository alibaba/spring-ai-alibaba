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

package com.alibaba.cloud.ai.studio.admin.annotation;

import org.springframework.aot.hint.annotation.Reflective;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * Title api model attribute.<br>
 * Description api model attribute.<br>
 *
 * @since 1.0.0.3
 */

@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Reflective
public @interface ApiModelAttribute {

	/**
	 * Alias for {@link #name}.
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the model attribute to bind to.
	 * <p>
	 * The default model attribute name is inferred from the declared attribute type (i.e.
	 * the method parameter type or method return type), based on the non-qualified class
	 * name: e.g. "orderAddress" for class "mypackage.OrderAddress", or "orderAddressList"
	 * for "List&lt;mypackage.OrderAddress&gt;".
	 * @since 4.3
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * Allows data binding to be disabled directly on an {@code @ModelAttribute} method
	 * parameter or on the attribute returned from an {@code @ModelAttribute} method, both
	 * of which would prevent data binding for that attribute.
	 * <p>
	 * By default this is set to {@code true} in which case data binding applies. Set this
	 * to {@code false} to disable data binding.
	 * @since 4.3
	 */
	boolean binding() default true;

}

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

package com.alibaba.cloud.ai.example.deepresearch.node.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The Node annotation is used to mark a class as a node in a graph or workflow. It
 * provides metadata about the node, including its name and description. This annotation
 * can be used to facilitate the identification and documentation of nodes within a
 * system.
 *
 * @author ViliamSun
 * @since 1.0.0
 */

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Node {

	String name();

	/**
	 * The description of the node, which can be used to provide additional context or
	 * information about the node's purpose. This description is typically used in user
	 * interfaces or documentation to help users understand what the node does.
	 * @return a string describing the node
	 */
	String description();

}

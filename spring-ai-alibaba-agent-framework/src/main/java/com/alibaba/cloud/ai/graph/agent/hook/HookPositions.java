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
package com.alibaba.cloud.ai.graph.agent.hook;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to specify the position(s) where a hook should be executed.
 * A hook can be executed at multiple positions by specifying multiple values.
 *
 * Example usage:
 * {@code
 * @HookPosition(HookPosition.BEFORE_AGENT)
 * public class MyBeforeAgentHook implements AgentHook { ... }
 *
 * @HookPosition({HookPosition.BEFORE_AGENT, HookPosition.AFTER_AGENT})
 * public class MyAgentHook implements AgentHook { ... }
 * }
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HookPositions {
	/**
	 * The positions where this hook should be executed
	 */
	HookPosition[] value();
}


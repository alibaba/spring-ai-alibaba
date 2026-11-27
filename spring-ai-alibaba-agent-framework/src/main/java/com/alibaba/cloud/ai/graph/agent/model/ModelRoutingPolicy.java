/*
 * Copyright 2025-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.graph.agent.model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;

import org.springframework.util.StringUtils;

/** Selects a named model once per graph execution. */
public final class ModelRoutingPolicy {

	private final String defaultRoute;

	private final Map<String, ModelTarget> routes;

	private final Function<ModelRequest, String> selector;

	private ModelRoutingPolicy(Builder builder) {
		this.defaultRoute = builder.defaultRoute;
		this.routes = Map.copyOf(builder.routes);
		this.selector = builder.selector;
		target(this.defaultRoute);
	}

	public static Builder builder(String defaultRoute) {
		return new Builder(defaultRoute);
	}

	public ModelTarget defaultTarget() {
		return target(this.defaultRoute);
	}

	/** Returns the default route when the selector returns null or blank. */
	public String select(ModelRequest request) {
		String name = this.selector.apply(request);
		return StringUtils.hasText(name) ? name : this.defaultRoute;
	}

	public ModelTarget target(String name) {
		var target = this.routes.get(name);
		if (target == null) {
			throw new IllegalArgumentException("Unknown model route: " + name);
		}
		return target;
	}

	public static final class Builder {

		private final String defaultRoute;

		private final Map<String, ModelTarget> routes = new LinkedHashMap<>();

		private Function<ModelRequest, String> selector = request -> null;

		private Builder(String defaultRoute) {
			if (!StringUtils.hasText(defaultRoute)) {
				throw new IllegalArgumentException("A default model route is required");
			}
			this.defaultRoute = defaultRoute;
		}

		public Builder route(String name, ModelTarget target) {
			if (!StringUtils.hasText(name) || this.routes.putIfAbsent(name,
					Objects.requireNonNull(target, "target must not be null")) != null) {
				throw new IllegalArgumentException("Model route names must be unique and non-blank");
			}
			return this;
		}

		public Builder selector(Function<ModelRequest, String> selector) {
			this.selector = Objects.requireNonNull(selector, "model route selector must not be null");
			return this;
		}

		public ModelRoutingPolicy build() {
			return new ModelRoutingPolicy(this);
		}

	}

}

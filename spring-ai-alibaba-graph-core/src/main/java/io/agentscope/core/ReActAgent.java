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
package io.agentscope.core;

import io.agentscope.core.memory.Memory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Flux;

import java.util.List;

public class ReActAgent {

	private final String name;

	private final String sysPrompt;

	private final Model model;

	private final Toolkit toolkit;

	private final Memory memory;

	private final boolean parallelToolCalls;

	private final int maxIters;

	public ReActAgent(String name, String sysPrompt, Model model, Toolkit toolkit, Memory memory,
			boolean parallelToolCalls, int maxIters) {
		this.name = name;
		this.sysPrompt = sysPrompt;
		this.model = model;
		this.toolkit = toolkit;
		this.memory = memory;
		this.parallelToolCalls = parallelToolCalls;
		this.maxIters = maxIters;
	}

	public Flux<Msg> stream(Msg msg) {
		return Flux.empty();
	}

	public Msg call(Msg msg) {
		return null;
	}

	public Flux<Msg> stream(List<Msg> msg) {
		return Flux.empty();
	}

	public Msg call(List<Msg> msg) {
		return null;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name;

		private String sysPrompt;

		private Model model;

		private Toolkit toolkit;

		private Memory memory;

		private boolean parallelToolCalls;

		private int maxIters;

		private Builder() {
		}

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder sysPrompt(String sysPrompt) {
			this.sysPrompt = sysPrompt;
			return this;
		}

		public Builder model(Model model) {
			this.model = model;
			return this;
		}

		public Builder toolkit(Toolkit toolkit) {
			this.toolkit = toolkit;
			return this;
		}

		public Builder memory(Memory memory) {
			this.memory = memory;
			return this;
		}

		public Builder parallelToolCalls(boolean parallelToolCalls) {
			this.parallelToolCalls = parallelToolCalls;
			return this;
		}

		public Builder maxIters(int maxIters) {
			this.maxIters = maxIters;
			return this;
		}

		public ReActAgent build() {
			return new ReActAgent(name, sysPrompt, model, toolkit, memory, parallelToolCalls, maxIters);
		}

	}

}

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
package io.agentscope.core.message;

public class Msg {

	private final String name;

	private final MsgRole role;

	private final ContentBlock content;

	private Msg(String name, MsgRole role, ContentBlock content) {
		this.name = name;
		this.role = role;
		this.content = content;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private String name;

		private MsgRole role;

		private ContentBlock content;

		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder role(MsgRole role) {
			this.role = role;
			return this;
		}

		public Builder content(ContentBlock content) {
			this.content = content;
			return this;
		}

		public Msg build() {
			return new Msg(name, role, content);
		}

	}

}

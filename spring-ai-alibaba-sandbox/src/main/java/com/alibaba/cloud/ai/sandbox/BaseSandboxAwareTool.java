/*
 * Copyright 2024-2026 the original author or authors.
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

package com.alibaba.cloud.ai.sandbox;

import io.agentscope.runtime.sandbox.box.Sandbox;
import io.agentscope.runtime.sandbox.tools.SandboxTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseSandboxAwareTool<T extends SandboxTool, Req, Res> implements SandboxAwareTool<Req, Res> {
    public static Logger logger = LoggerFactory.getLogger(BaseSandboxAwareTool.class);
	protected T sandboxTool;

	public BaseSandboxAwareTool(T sandboxTool) {
		this.sandboxTool = sandboxTool;
	}

	@Override
	public Sandbox getSandbox() {
		return this.sandboxTool.getSandbox();
	}

	@Override
	public void setSandbox(Sandbox sandbox) {
		this.sandboxTool.setSandbox(sandbox);
	}

	@Override
	public Class<?> getSandboxClass() {
		return sandboxTool.getSandboxClass();
	}
}

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
package com.alibaba.cloud.ai.graph.agent.renderer;

import org.slf4j.Logger;
import org.stringtemplate.v4.STErrorListener;
import org.stringtemplate.v4.misc.ErrorType;
import org.stringtemplate.v4.misc.STMessage;

/**
 * An {@link STErrorListener} that delegates to {@link Logger slf4j}.
 *
 * @author Sun Yuhan
 */
public class Slf4jStErrorListener implements STErrorListener {

	private final Logger logger;

	/* package */ Slf4jStErrorListener(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void compileTimeError(STMessage msg) {
		logger.error(msg.toString());
	}

	@Override
	public void runTimeError(STMessage msg) {
		if (msg.error != ErrorType.NO_SUCH_PROPERTY) { // ignore these
			logger.error(msg.toString());
		}
		else {
			logger.warn(msg.toString());
		}
	}

	@Override
	public void IOError(STMessage msg) {
		logger.error(msg.toString());
	}

	@Override
	public void internalError(STMessage msg) {
		logger.error(msg.toString());
	}

}

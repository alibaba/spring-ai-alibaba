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
package com.alibaba.cloud.ai.vo;

import com.alibaba.cloud.ai.param.ModelRunActionParam;

public class ChatModelRunResult {

	private ModelRunActionParam input;

	private ActionResult result;

	private TelemetryResult telemetry;

	public ChatModelRunResult() {
	}

	public ChatModelRunResult(ModelRunActionParam input, ActionResult result, TelemetryResult telemetry) {
		this.input = input;
		this.result = result;
		this.telemetry = telemetry;
	}

	public ModelRunActionParam getInput() {
		return input;
	}

	public void setInput(ModelRunActionParam input) {
		this.input = input;
	}

	public ActionResult getResult() {
		return result;
	}

	public void setResult(ActionResult result) {
		this.result = result;
	}

	public TelemetryResult getTelemetry() {
		return telemetry;
	}

	public void setTelemetry(TelemetryResult telemetry) {
		this.telemetry = telemetry;
	}

	@Override
	public String toString() {
		return "ChatModelRunResult{" + "input=" + input + ", result=" + result + ", telemetry=" + telemetry + '}';
	}

	public static ChatModelRunResultBuilder builder() {
		return new ChatModelRunResultBuilder();
	}

	public static final class ChatModelRunResultBuilder {

		private ModelRunActionParam input;

		private ActionResult result;

		private TelemetryResult telemetry;

		private ChatModelRunResultBuilder() {
		}

		public static ChatModelRunResultBuilder aChatModelRunResult() {
			return new ChatModelRunResultBuilder();
		}

		public ChatModelRunResultBuilder input(ModelRunActionParam input) {
			this.input = input;
			return this;
		}

		public ChatModelRunResultBuilder result(ActionResult result) {
			this.result = result;
			return this;
		}

		public ChatModelRunResultBuilder telemetry(TelemetryResult telemetry) {
			this.telemetry = telemetry;
			return this;
		}

		public ChatModelRunResult build() {
			ChatModelRunResult chatModelRunResult = new ChatModelRunResult();
			chatModelRunResult.setInput(input);
			chatModelRunResult.setResult(result);
			chatModelRunResult.setTelemetry(telemetry);
			return chatModelRunResult;
		}

	}

}

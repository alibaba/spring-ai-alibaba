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

import com.alibaba.cloud.ai.param.ClientRunActionParam;

public class ChatClientRunResult {

	private ClientRunActionParam input;

	private ActionResult result;

	private TelemetryResult telemetry;

	private String ChatID;

	public ClientRunActionParam getInput() {
		return input;
	}

	public void setInput(ClientRunActionParam input) {
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

	public String getChatID() {
		return ChatID;
	}

	public void setChatID(String chatID) {
		ChatID = chatID;
	}

	@Override
	public String toString() {
		return "ChatClientRunResult{" + "input=" + input + ", result=" + result + ", telemetry=" + telemetry
				+ ", ChatID='" + ChatID + '\'' + '}';
	}

	public static ChatClientRunResultBuilder builder() {
		return new ChatClientRunResultBuilder();
	}

	public static final class ChatClientRunResultBuilder {

		private ClientRunActionParam input;

		private ActionResult result;

		private TelemetryResult telemetry;

		private String ChatID;

		private ChatClientRunResultBuilder() {
		}

		public static ChatClientRunResultBuilder aChatClientRunResult() {
			return new ChatClientRunResultBuilder();
		}

		public ChatClientRunResultBuilder input(ClientRunActionParam input) {
			this.input = input;
			return this;
		}

		public ChatClientRunResultBuilder result(ActionResult result) {
			this.result = result;
			return this;
		}

		public ChatClientRunResultBuilder telemetry(TelemetryResult telemetry) {
			this.telemetry = telemetry;
			return this;
		}

		public ChatClientRunResultBuilder ChatID(String ChatID) {
			this.ChatID = ChatID;
			return this;
		}

		public ChatClientRunResult build() {
			ChatClientRunResult chatClientRunResult = new ChatClientRunResult();
			chatClientRunResult.setInput(input);
			chatClientRunResult.setResult(result);
			chatClientRunResult.setTelemetry(telemetry);
			chatClientRunResult.setChatID(ChatID);
			return chatClientRunResult;
		}

	}

}

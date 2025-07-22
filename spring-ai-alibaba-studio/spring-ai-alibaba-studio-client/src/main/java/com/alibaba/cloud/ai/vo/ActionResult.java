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

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class ActionResult {

	private String Response;

	@Schema(description = "stream response", nullable = true)
	private List<String> streamResponse;

	public String getResponse() {
		return Response;
	}

	public void setResponse(String response) {
		Response = response;
	}

	public List<String> getStreamResponse() {
		return streamResponse;
	}

	public void setStreamResponse(List<String> streamResponse) {
		this.streamResponse = streamResponse;
	}

	@Override
	public String toString() {
		return "ActionResult{" + "Response='" + Response + '\'' + ", streamResponse=" + streamResponse + '}';
	}

	public static ActionResultBuilder builder() {
		return new ActionResultBuilder();
	}

	public static final class ActionResultBuilder {

		private String Response;

		private List<String> streamResponse;

		private ActionResultBuilder() {
		}

		public static ActionResultBuilder anActionResult() {
			return new ActionResultBuilder();
		}

		public ActionResultBuilder Response(String Response) {
			this.Response = Response;
			return this;
		}

		public ActionResultBuilder streamResponse(List<String> streamResponse) {
			this.streamResponse = streamResponse;
			return this;
		}

		public ActionResult build() {
			ActionResult actionResult = new ActionResult();
			actionResult.setResponse(Response);
			actionResult.setStreamResponse(streamResponse);
			return actionResult;
		}

	}

}

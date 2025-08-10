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

package com.alibaba.cloud.ai.example.deepresearch.model;

import com.alibaba.cloud.ai.example.deepresearch.model.req.GraphId;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.List;

/**
 * 会话记录，用于存储用户与LLM的交互结果
 */
public class SessionHistory {

	private GraphId graphId;

	private String userQuery;

	private String report;

	private SessionHistory(GraphId graphId, String userQuery, String report) {
		this.graphId = graphId;
		this.userQuery = userQuery;
		this.report = report;
	}

	public GraphId getGraphId() {
		return graphId;
	}

	public void setGraphId(GraphId graphId) {
		this.graphId = graphId;
	}

	public String getUserQuery() {
		return userQuery;
	}

	public void setUserQuery(String userQuery) {
		this.userQuery = userQuery;
	}

	public String getReport() {
		return report;
	}

	public void setReport(String report) {
		this.report = report;
	}

	@Override
	public String toString() {
		return "SessionHistory{" + "userQuery='" + userQuery + '\'' + ", report='" + report + '\'' + '}';
	}

	/**
	 * 将会话记录转为UserMessage和AssistantMessage
	 */
	public List<Message> convertToMessage() {
		return List.of(new UserMessage(userQuery), new AssistantMessage(report));
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {

		private GraphId graphId;

		private String userQuery;

		private String report;

		public SessionHistory build() {
			return new SessionHistory(graphId, userQuery, report);
		}

		public Builder graphId(GraphId graphId) {
			this.graphId = graphId;
			return this;
		}

		public Builder sessionId(String sessionId) {
			this.graphId = new GraphId(sessionId, this.graphId == null ? null : this.graphId.threadId());
			return this;
		}

		public Builder threadId(String threadId) {
			this.graphId = new GraphId(this.graphId == null ? null : this.graphId.sessionId(), threadId);
			return this;
		}

		public Builder userQuery(String userQuery) {
			this.userQuery = userQuery;
			return this;
		}

		public Builder report(String report) {
			this.report = report;
			return this;
		}

	}

}

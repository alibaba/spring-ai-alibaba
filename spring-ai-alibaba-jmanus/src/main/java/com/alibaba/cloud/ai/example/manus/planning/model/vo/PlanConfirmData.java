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
package com.alibaba.cloud.ai.example.manus.planning.model.vo;

import java.io.Serializable;
import java.util.Map;

/**
 * confirm plan data
 */
public class PlanConfirmData implements Serializable {

	/**
	 * plan id
	 */
	private String planId;

	/**
	 * plan confirm result, see ConfirmState
	 */
	private String accepted;

	/**
	 * confirm type，see ConfirmType
	 */
	private String type;

	/**
	 * confirm time
	 */
	private long time;

	public PlanConfirmData(String planId, String accepted, String type, long time) {
		this.planId = planId;
		this.accepted = accepted;
		this.type = type;
		this.time = time;
	}

	public PlanConfirmData(Map<String, String> map) {
		this.planId = map.get("planId");
		this.accepted = String.valueOf(map.get("accepted"));
		this.type = ConfirmType.USER.getType();
		this.time = System.currentTimeMillis();
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public String getAccepted() {
		return accepted;
	}

	public void setAccepted(String accepted) {
		this.accepted = accepted;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public enum ConfirmType {

		USER("user"),

		TIMEOUT("timeout"),

		EXCEPTION("exception");

		private String type;

		ConfirmType(String type) {
			this.type = type;
		}

		public String getType() {
			return type;
		}

	}

	public enum ConfirmState {

		ACCEPT("accept"),

		REJECT("reject"),

		AWAIT("await");

		private String state;

		ConfirmState(String state) {
			this.state = state;
		}

		public String getState() {
			return state;
		}

		public static ConfirmState values(String state) {
			for (ConfirmState confirmState : ConfirmState.values()) {
				if (confirmState.getState().equals(state)) {
					return confirmState;
				}
			}
			return null;
		}

	}

}

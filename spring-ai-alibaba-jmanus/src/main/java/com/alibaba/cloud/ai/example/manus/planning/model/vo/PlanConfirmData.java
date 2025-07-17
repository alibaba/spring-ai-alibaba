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
	 * plan confirm or not
	 */
	private Boolean accepted;

	/**
	 * confirm type，user/timeout/exception
	 */
	private String type;

	/**
	 * confirm time
	 */
	private long time;

	public PlanConfirmData(String planId, Boolean accepted, String type, long time) {
		this.planId = planId;
		this.accepted = accepted;
		this.type = type;
		this.time = time;
	}

	public PlanConfirmData(Map<String, String> map) {
		if (map == null || map.isEmpty()) {
			throw new IllegalArgumentException("map is null or empty");
		}
		if (!map.containsKey("planId") || !map.containsKey("accepted")) {
			throw new IllegalArgumentException("map is not valid");
		}
		this.planId = map.get("planId");
		this.accepted = Boolean.valueOf(map.get("accepted"));
		this.type = "user";
		this.time = System.currentTimeMillis();
	}

	public String getPlanId() {
		return planId;
	}

	public void setPlanId(String planId) {
		this.planId = planId;
	}

	public Boolean getAccepted() {
		return accepted;
	}

	public void setAccepted(Boolean accepted) {
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

}

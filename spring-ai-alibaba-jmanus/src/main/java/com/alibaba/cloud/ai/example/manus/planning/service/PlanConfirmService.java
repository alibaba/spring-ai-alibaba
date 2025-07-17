package com.alibaba.cloud.ai.example.manus.planning.service;

import com.alibaba.cloud.ai.example.manus.planning.model.vo.PlanConfirmData;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * user confirm plan service
 */
@Service
public class PlanConfirmService {

	/**
	 * plan confirm data
	 */
	private final ConcurrentHashMap<String, PlanConfirmData> planConfirmDataMap = new ConcurrentHashMap<>();

	/**
	 * store confirm data
	 * @param planId plan id
	 * @param data confirm data
	 */
	public void storeConfirmData(String planId, PlanConfirmData data) {
		planConfirmDataMap.put(planId, data);
	}

	/**
	 * get confirm data
	 * @param planId plan id
	 * @return confirm data
	 */
	public PlanConfirmData getConfirmData(String planId) {
		return planConfirmDataMap.get(planId);
	}

	/**
	 * remove confirm data
	 * @param planId plan id
	 */
	public void removeConfirmDataMap(String planId) {
		planConfirmDataMap.remove(planId);
	}

}

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

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
package com.alibaba.cloud.ai.manus.tool.browser;

import com.alibaba.cloud.ai.manus.config.IManusProperties;
import com.alibaba.cloud.ai.manus.tool.innerStorage.SmartContentSavingService;
import com.alibaba.cloud.ai.manus.tool.filesystem.UnifiedDirectoryManager;

/**
 * Chrome driver service interface providing browser driver management functions
 */
public interface IChromeDriverService {

	/**
	 * Get shared directory
	 * @return Shared directory path
	 */
	String getSharedDir();

	/**
	 * Save cookies from all drivers to global shared directory
	 */
	void saveCookiesToSharedDir();

	/**
	 * Load cookies from global shared directory to all drivers
	 */
	void loadCookiesFromSharedDir();

	/**
	 * Get driver wrapper for specified plan ID
	 * @param planId Plan ID
	 * @return Driver wrapper
	 */
	DriverWrapper getDriver(String planId);

	/**
	 * Close driver for specified plan
	 * @param planId Plan ID
	 */
	void closeDriverForPlan(String planId);

	/**
	 * Clean up all resources
	 */
	void cleanup();

	/**
	 * Set Manus properties
	 * @param manusProperties Manus properties
	 */
	void setManusProperties(IManusProperties manusProperties);

	/**
	 * Get Manus properties
	 * @return Manus properties
	 */
	IManusProperties getManusProperties();

	/**
	 * Get internal storage service
	 * @return Internal storage service
	 */
	SmartContentSavingService getInnerStorageService();

	/**
	 * Get unified directory manager
	 * @return Unified directory manager
	 */
	UnifiedDirectoryManager getUnifiedDirectoryManager();

}

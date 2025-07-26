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
package com.alibaba.cloud.ai.vectorstore.tablestore;

import com.alicloud.openservices.tablestore.SyncClient;
import org.junit.jupiter.api.Assumptions;

class EnvUtil {

	private static volatile SyncClient client;

	static SyncClient getClient() {
		if (client == null) {
			synchronized (EnvUtil.class) {
				if (client == null) {
					String endPoint = System.getenv("tablestore_end_point");
					String instanceName = System.getenv("tablestore_instance_name");
					String accessKeyId = System.getenv("tablestore_access_key_id");
					String accessKeySecret = System.getenv("tablestore_access_key_secret");
					if (endPoint == null || instanceName == null || accessKeyId == null || accessKeySecret == null) {
						Assumptions.abort(
								"env tablestore_end_point, tablestore_instance_name, tablestore_access_key_id, tablestore_access_key_secret is not set");
					}
					client = new SyncClient(endPoint, accessKeyId, accessKeySecret, instanceName);
				}
			}
		}
		return client;
	}

}

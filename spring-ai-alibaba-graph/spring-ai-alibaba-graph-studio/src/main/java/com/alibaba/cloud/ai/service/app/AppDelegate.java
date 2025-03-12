/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.service.app;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.param.CreateAppParam;

import java.util.List;

/**
 * AppDelegate defines the app crud operations.
 */
public interface AppDelegate {

	App create(CreateAppParam param);

	App get(String id);

	List<App> list();

	App sync(App app);

	void delete(String id);

}

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
package com.alibaba.cloud.ai.saver;

import com.alibaba.cloud.ai.model.App;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple memory persistence implementation of {@link AppSaver}
 */
public class AppMemorySaver implements AppSaver {

	private Map<String, App> apps;

	public AppMemorySaver() {
		this.apps = new ConcurrentHashMap<>();
	}

	public AppMemorySaver(Map<String, App> apps) {
		this.apps = new ConcurrentHashMap<>(apps);
	}

	@Override
	public List<App> list() {
		return apps.values().stream().toList();
	}

	@Override
	public App get(String id) {
		return apps.get(id);
	}

	@Override
	public App save(App app) {
		apps.put(app.getMetadata().getId(), app);
		return app;
	}

	@Override
	public void delete(String id) {
		apps.remove(id);
	}

}

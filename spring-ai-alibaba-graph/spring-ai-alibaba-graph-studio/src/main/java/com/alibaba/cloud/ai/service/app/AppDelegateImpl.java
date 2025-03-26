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
package com.alibaba.cloud.ai.service.app;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.model.AppMetadata;
import com.alibaba.cloud.ai.param.CreateAppParam;
import com.alibaba.cloud.ai.saver.AppSaver;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class AppDelegateImpl implements AppDelegate {

	private AppSaver appSaver;

	public AppDelegateImpl(AppSaver appSaver) {
		this.appSaver = appSaver;
	}

	@Override
	public App create(CreateAppParam param) {
		if (!Arrays.asList(AppMetadata.SUPPORT_MODES).contains(param.getMode())) {
			throw new IllegalArgumentException("unsupported app mode: " + param.getMode());
		}
		AppMetadata metadata = new AppMetadata().setId(UUID.randomUUID().toString())
			.setName(param.getName())
			.setMode(param.getMode())
			.setDescription(param.getDescription());
		App app = new App(metadata, null);
		return appSaver.save(app);
	}

	@Override
	public App get(String id) {
		return appSaver.get(id);
	}

	@Override
	public List<App> list() {
		return appSaver.list();
	}

	@Override
	public App sync(App app) {
		return appSaver.save(app);
	}

	@Override
	public void delete(String id) {
		appSaver.delete(id);
	}

}

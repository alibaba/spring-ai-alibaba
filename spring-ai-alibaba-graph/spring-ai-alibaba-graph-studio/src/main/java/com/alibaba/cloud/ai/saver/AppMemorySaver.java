package com.alibaba.cloud.ai.saver;

import com.alibaba.cloud.ai.model.App;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AppMemorySaver implements AppSaver {

	private Map<String, App> apps;

	public AppMemorySaver() {
		this.apps = new ConcurrentHashMap<>();
	}

	public AppMemorySaver(Map<String, App> apps) {
		this.apps = apps;
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
	public App create(App app) {
		if (apps.containsKey(app.getMetadata().getId())) {
			throw new IllegalArgumentException("app id already exists");
		}
		return apps.put(app.getMetadata().getId(), app);
	}

	@Override
	public App update(App app) {
		if (!apps.containsKey(app.getMetadata().getId())) {
			throw new IllegalArgumentException("app not exists: " + app.getMetadata().getId());
		}
		return apps.put(app.getMetadata().getId(), app);

	}

	@Override
	public void delete(String id) {
		apps.remove(id);
	}

}

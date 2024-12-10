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

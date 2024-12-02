package com.alibaba.cloud.ai.store;

import com.alibaba.cloud.ai.model.app.App;

import java.util.List;

public interface AppSaver {

	List<App> list();

	App get(String id);

	App create(App app);

	App update(App app);

	void delete(String id);

}

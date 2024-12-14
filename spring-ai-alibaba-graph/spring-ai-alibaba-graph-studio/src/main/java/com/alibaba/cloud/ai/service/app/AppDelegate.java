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

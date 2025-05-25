package com.alibaba.cloud.ai.saver;

import com.alibaba.cloud.ai.model.App;

import java.util.List;

/**
 * AppSaver is the abstraction of the persistence of {@link App}
 */
public interface AppSaver {

	List<App> list();

	App get(String id);

	App save(App app);

	void delete(String id);

}

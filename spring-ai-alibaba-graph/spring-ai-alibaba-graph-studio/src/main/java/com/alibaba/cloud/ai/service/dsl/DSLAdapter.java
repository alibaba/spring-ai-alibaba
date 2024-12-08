package com.alibaba.cloud.ai.service.dsl;

import com.alibaba.cloud.ai.model.App;
import com.alibaba.cloud.ai.saver.AppSaver;

public interface DSLAdapter {

	String exportDSL(String id, AppSaver appSaver);

	// turn DSL into app
	App importDSL(String dsl);

	// turn DSL into app and save imported app
	App importDSL(String dsl, AppSaver appSaver);

	Boolean supportDialect(String dialect);

}

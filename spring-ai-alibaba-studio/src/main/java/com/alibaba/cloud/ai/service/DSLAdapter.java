package com.alibaba.cloud.ai.service;

import com.alibaba.cloud.ai.model.app.App;

public interface DSLAdapter {

	String exportDSL(String id);

	App importDSL(String dsl);

	Boolean supportDialect(String dialect);

}

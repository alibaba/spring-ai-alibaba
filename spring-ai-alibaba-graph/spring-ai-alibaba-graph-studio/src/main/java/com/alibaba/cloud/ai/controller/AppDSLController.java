package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.DSLAPI;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.dsl.DSLAdapter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("graph-studio/api/dsl")
public class AppDSLController implements DSLAPI {

	private final List<DSLAdapter> adapters;

	private final AppSaver appSaver;

	public AppDSLController(AppSaver appSaver, List<DSLAdapter> adapters) {
		this.appSaver = appSaver;
		this.adapters = adapters;
	}

	@Override
	public DSLAdapter getAdapter(String dialect) {
		for (DSLAdapter adapter : adapters) {
			if (adapter.supportDialect(dialect)) {
				return adapter;
			}
		}
		return null;
	}

	@Override
	public AppSaver getAppSaver() {
		return appSaver;
	}

}

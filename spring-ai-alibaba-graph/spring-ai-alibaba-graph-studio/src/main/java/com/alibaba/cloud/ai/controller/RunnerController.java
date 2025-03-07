package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.RunnerAPI;
import com.alibaba.cloud.ai.saver.AppSaver;
import com.alibaba.cloud.ai.service.runner.Runner;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("graph-studio/run")
public class RunnerController implements RunnerAPI {

	private final List<Runner> runners;

	private final AppSaver appSaver;

	public RunnerController(List<Runner> runners, AppSaver appSaver) {
		this.runners = runners;
		this.appSaver = appSaver;
	}

	@Override
	public Runner getRunner(String type) {
		return runners.stream().filter(r -> r.support(type)).findFirst().orElse(null);
	}

	@Override
	public AppSaver getAppSaver() {
		return appSaver;
	}

}

package com.alibaba.cloud.ai.controller;

import com.alibaba.cloud.ai.api.AppAPI;
import com.alibaba.cloud.ai.service.app.AppDelegate;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin
@RestController
@RequestMapping("graph-studio/api/app")
public class AppController implements AppAPI {

	private final AppDelegate delegate;

	public AppController(AppDelegate delegate) {
		this.delegate = delegate;
	}

	@Override
	public AppDelegate getDelegate() {
		return delegate;
	}

}

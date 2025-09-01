package com.alibaba.cloud.ai.a2a.server;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

/**
 * @author xiweng.yy
 */
public class DefaultA2aServerExecutorProvider implements A2aServerExecutorProvider {

	private final ExecutorService executor;

	public DefaultA2aServerExecutorProvider() {
		this.executor = Executors.newCachedThreadPool();
		;
	}

	@PreDestroy
	public void close() {
		executor.shutdown();
	}

	@Override
	public ExecutorService getA2aServerExecutor() {
		return executor;
	}

}

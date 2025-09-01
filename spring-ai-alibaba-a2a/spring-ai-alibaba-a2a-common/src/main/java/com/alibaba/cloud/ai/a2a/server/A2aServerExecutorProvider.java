package com.alibaba.cloud.ai.a2a.server;

import java.util.concurrent.ExecutorService;

/**
 * @author xiweng.yy
 */
public interface A2aServerExecutorProvider {

	ExecutorService getA2aServerExecutor();

}

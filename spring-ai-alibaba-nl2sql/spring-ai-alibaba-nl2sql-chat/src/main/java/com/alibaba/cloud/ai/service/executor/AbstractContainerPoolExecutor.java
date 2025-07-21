/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.service.executor;

import com.alibaba.cloud.ai.config.ContainerProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 运行Python任务的容器池
 *
 * @author vlsmb
 * @since 2025/7/12
 */
public abstract class AbstractContainerPoolExecutor implements ContainerPoolExecutor {

	private static final Logger log = LoggerFactory.getLogger(AbstractContainerPoolExecutor.class);

	// 记录核心容器的状态
	protected final ConcurrentHashMap<String, ContainerPoolExecutor.State> coreContainerState;

	// 记录临时容器的状态
	protected final ConcurrentHashMap<String, ContainerPoolExecutor.State> tempContainerState;

	// 记录临时容器销毁的Future
	protected final ConcurrentHashMap<String, Future<?>> tempContainerRemoveFuture;

	// 任务队列（当容器满时临时存放任务）
	protected final ArrayBlockingQueue<FutureTask<ContainerPoolExecutor.TaskResponse>> taskQueue;

	// 已经就绪的核心容器
	protected final ArrayBlockingQueue<String> readyCoreContainer;

	// 已经就绪的临时容器
	protected final ArrayBlockingQueue<String> readyTempContainer;

	// 当前核心容器的数量
	protected final AtomicInteger currentCoreContainerSize;

	// 当前临时容器的数量
	protected final AtomicInteger currentTempContainerSize;

	// 线程池，运行临时存放的任务
	protected final ExecutorService consumerThreadPool;

	// 配置属性
	protected final ContainerProperties properties;

	public AbstractContainerPoolExecutor(ContainerProperties properties) {
		this.properties = properties;
		this.coreContainerState = new ConcurrentHashMap<>();
		this.tempContainerState = new ConcurrentHashMap<>();
		this.tempContainerRemoveFuture = new ConcurrentHashMap<>();
		this.taskQueue = new ArrayBlockingQueue<>(properties.getTaskQueueSize());
		this.readyCoreContainer = new ArrayBlockingQueue<>(properties.getCoreContainerNum());
		this.readyTempContainer = new ArrayBlockingQueue<>(properties.getTempContainerNum());
		this.consumerThreadPool = new ThreadPoolExecutor(properties.getCoreThreadSize(), properties.getMaxThreadSize(),
				properties.getKeepThreadAliveTime(), TimeUnit.SECONDS,
				new ArrayBlockingQueue<>(properties.getThreadQueueSize()));
		this.currentCoreContainerSize = new AtomicInteger(0);
		this.currentTempContainerSize = new AtomicInteger(0);
		// 注册关闭钩子
		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			log.info("Shutting down container pool executor...");
			try {
				this.shutdownPool();
			}
			catch (Exception ignored) {
			}
		}));
	}

	protected abstract String createNewContainer() throws Exception;

	protected abstract TaskResponse execTaskInContainer(TaskRequest request, String containerId) throws Exception;

	protected abstract void stopContainer(String containerId) throws Exception;

	protected abstract void removeContainer(String containerId) throws Exception;

	protected void shutdownPool() throws Exception {
		// 关闭线程池
		this.consumerThreadPool.shutdownNow();
		// 停止并删除所有的容器
		for (String containerId : this.tempContainerState.keySet()) {
			try {
				this.stopContainer(containerId);
				this.removeContainer(containerId);
			}
			catch (Exception ignored) {

			}
		}
		for (String containerId : this.coreContainerState.keySet()) {
			try {
				this.stopContainer(containerId);
				this.removeContainer(containerId);
			}
			catch (Exception ignored) {

			}
		}
		this.tempContainerState.clear();
		this.coreContainerState.clear();
		this.tempContainerRemoveFuture.clear();
		this.readyCoreContainer.clear();
		this.readyTempContainer.clear();
		this.taskQueue.clear();
	}

	// 创建删除临时容器的线程
	private Future<?> registerRemoveTempContainer(String containerId) {
		return consumerThreadPool.submit(() -> {
			try {
				Thread.sleep(this.properties.getKeepThreadAliveTime() * 1000L * 60L);
				if (this.tempContainerState.get(containerId) == State.RUNNING) {
					throw new InterruptedException("Container " + containerId + " is already running");
				}
			}
			catch (InterruptedException e) {
				// 由于容器正在运行而取消临时容器的销毁线程
				log.debug("Interrupted while waiting for temp container to be removed, info: {}", e.getMessage());
				return;
			}
			try {
				// 移除临时容器
				this.tempContainerState.remove(containerId);
				this.tempContainerRemoveFuture.remove(containerId);
				this.removeContainer(containerId);
				log.debug("Container {} has been removed successfully", containerId);
			}
			catch (Exception e) {
				log.error("Error when trying to register temp container to be removed, containerId: {}, info: {}",
						containerId, e.getMessage(), e);
			}
		});
	}

	// 使用核心容器
	private TaskResponse useCoreContainer(String containerId, TaskRequest request) {
		try {
			// 执行任务
			this.coreContainerState.replace(containerId, State.RUNNING);
			TaskResponse resp = this.execTaskInContainer(request, containerId);
			this.coreContainerState.replace(containerId, State.READY);
			// 放回阻塞队列中
			this.readyCoreContainer.add(containerId);
			// 运行任务队列里的任务，如果有
			this.popTaskQueue();
			return resp;
		}
		catch (Exception e) {
			log.error("use core container failed, {}", e.getMessage(), e);
			return TaskResponse.error(e.getMessage());
		}
	}

	// 使用临时容器
	private TaskResponse useTempContainer(String containerId, TaskRequest request) {
		try {
			Future<?> future = this.tempContainerRemoveFuture.remove(containerId);
			// 取消临时容器的销毁线程
			if (future != null) {
				if (future.isDone()) {
					// 容器被销毁，重新选择使用策略
					log.debug("reselect strategy: {} ...", request.toString());
					return this.runTask(request);
				}
				future.cancel(true);
			}
			// 执行任务
			this.tempContainerState.replace(containerId, State.RUNNING);
			TaskResponse resp = this.execTaskInContainer(request, containerId);
			this.tempContainerState.replace(containerId, State.READY);
			// 放回阻塞队列中
			this.readyTempContainer.add(containerId);
			// 重新创建临时容器的销毁线程
			this.tempContainerRemoveFuture.put(containerId, this.registerRemoveTempContainer(containerId));
			// 运行任务队列里的任务，如果有
			this.popTaskQueue();
			return resp;
		}
		catch (Exception e) {
			log.error("use temp container failed, {}", e.getMessage(), e);
			return TaskResponse.error(e.getMessage());
		}
	}

	// 创建并使用核心容器
	private TaskResponse createAndUseCoreContainer(TaskRequest request) {
		String containerId;
		try {
			containerId = this.createNewContainer();
		}
		catch (Exception e) {
			log.error("create new container failed, {}", e.getMessage(), e);
			return TaskResponse.error(e.getMessage());
		}
		// 记录新增的容器
		this.coreContainerState.put(containerId, State.READY);
		// 使用容器
		return this.useCoreContainer(containerId, request);
	}

	// 创建并使用临时容器
	private TaskResponse createAndUseTempContainer(TaskRequest request) {
		String containerId;
		try {
			containerId = this.createNewContainer();
		}
		catch (Exception e) {
			log.error("create new container failed, {}", e.getMessage(), e);
			return TaskResponse.error(e.getMessage());
		}
		// 记录新增的容器
		this.tempContainerState.put(containerId, State.READY);
		// 使用容器
		return this.useTempContainer(containerId, request);
	}

	private TaskResponse pushTaskQueue(TaskRequest request) throws ExecutionException, InterruptedException {
		FutureTask<ContainerPoolExecutor.TaskResponse> ft = new FutureTask<>(() -> {
			log.info("Execute tasks in the BlockingQueue {} ...", request.toString());
			return this.runTask(request);
		});
		this.taskQueue.put(ft);
		return ft.get();
	}

	// 运行任务队列里的任务，如果有
	private void popTaskQueue() {
		FutureTask<ContainerPoolExecutor.TaskResponse> future = this.taskQueue.poll();
		if (future == null) {
			return;
		}
		log.info("Get task from the BlockingQueue ...");
		this.consumerThreadPool.submit(future);
	}

	@Override
	public TaskResponse runTask(TaskRequest request) {
		// 使用可用的核心容器
		String freeCoreId = this.readyCoreContainer.poll();
		if (freeCoreId != null) {
			log.debug("Use free core container to run task {} ...", request.toString());
			return this.useCoreContainer(freeCoreId, request);
		}

		// 使用可用的临时容器
		String freeTempId = this.readyTempContainer.poll();
		if (freeTempId != null) {
			log.debug("Use free temp container to run task {} ...", request.toString());
			return this.useTempContainer(freeTempId, request);
		}

		// 创建新的核心容器
		int currentCore;
		boolean useCoreContainer = true;
		do {
			currentCore = this.currentCoreContainerSize.get();
			if (currentCore >= properties.getCoreContainerNum()) {
				useCoreContainer = false;
				break;
			}
		}
		while (!this.currentCoreContainerSize.compareAndSet(currentCore, currentCore + 1));
		if (useCoreContainer) {
			log.debug("Create new core container to run task {} ...", request.toString());
			return this.createAndUseCoreContainer(request);
		}

		// 创建新的临时容器
		int currentTemp;
		boolean useTempContainer = true;
		do {
			currentTemp = this.currentTempContainerSize.get();
			if (currentTemp >= properties.getTempContainerNum()) {
				useTempContainer = false;
				break;
			}
		}
		while (!this.currentTempContainerSize.compareAndSet(currentTemp, currentTemp + 1));
		if (useTempContainer) {
			log.debug("Create new temp container to run task {} ...", request.toString());
			return this.createAndUseTempContainer(request);
		}

		// 放入任务队列里等待
		try {
			log.debug("push task into BlockingQueue: {} ...", request.toString());
			return this.pushTaskQueue(request);
		}
		catch (Exception e) {
			log.error("An exception occurred while executing the task: {}", e.getMessage(), e);
			return TaskResponse.error(e.getMessage());
		}
	}

	/**
	 * 删除临时目录
	 */
	protected void clearTempDir(Path tempDir) {
		try {
			Files.walkFileTree(tempDir, new SimpleFileVisitor<>() {
				@NotNull
				@Override
				public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs)
						throws IOException {
					Files.delete(file);
					return super.visitFile(file, attrs);
				}

				@NotNull
				@Override
				public FileVisitResult postVisitDirectory(@NotNull Path dir, @Nullable IOException exc)
						throws IOException {
					if (exc != null)
						throw exc;
					Files.delete(dir);
					return super.postVisitDirectory(dir, exc);
				}
			});
			log.info("Temp directory has been deleted.");
		}
		catch (Exception e) {
			log.warn("Exception in clean temp directory: {}", e.getMessage());
		}
	}

	/**
	 * 生成唯一的容器名称
	 */
	protected String generateContainerName() {
		return this.properties.getContainerNamePrefix() + "_" + System.currentTimeMillis() + "_"
				+ Thread.currentThread().getName();
	}

}

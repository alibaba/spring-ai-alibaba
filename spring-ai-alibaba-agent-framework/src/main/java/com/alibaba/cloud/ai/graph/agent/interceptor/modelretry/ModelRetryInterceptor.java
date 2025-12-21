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
package com.alibaba.cloud.ai.graph.agent.interceptor.modelretry;

import com.alibaba.cloud.ai.graph.agent.interceptor.ModelCallHandler;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelInterceptor;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelRequest;
import com.alibaba.cloud.ai.graph.agent.interceptor.ModelResponse;

import org.springframework.ai.chat.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * 模型调用重试拦截器，用于处理网络错误等可重试的异常。
 *
 * 当模型调用失败时，会根据配置的重试策略进行重试，直到成功或达到最大重试次数。
 *
 * 示例:
 * <pre>
 * ModelRetryInterceptor interceptor = ModelRetryInterceptor.builder()
 *     .maxAttempts(3)
 *     .initialDelay(1000)
 *     .maxDelay(10000)
 *     .backoffMultiplier(2.0)
 *     .build();
 * </pre>
 */
public class ModelRetryInterceptor extends ModelInterceptor {

	private static final Logger log = LoggerFactory.getLogger(ModelRetryInterceptor.class);

	private final int maxAttempts;
	private final long initialDelay;
	private final long maxDelay;
	private final double backoffMultiplier;
	private final Predicate<Exception> retryableExceptionPredicate;

	private ModelRetryInterceptor(Builder builder) {
		this.maxAttempts = builder.maxAttempts;
		this.initialDelay = builder.initialDelay;
		this.maxDelay = builder.maxDelay;
		this.backoffMultiplier = builder.backoffMultiplier;
		this.retryableExceptionPredicate = builder.retryableExceptionPredicate;
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public ModelResponse interceptModel(ModelRequest request, ModelCallHandler handler) {
		Exception lastException = null;
		long currentDelay = initialDelay;

		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			try {
				if (attempt > 1) {
					log.info("重试模型调用，第 {} 次尝试（共 {} 次）", attempt, maxAttempts);
				}

				ModelResponse modelResponse = handler.call(request);
				Message message = (Message) modelResponse.getMessage();

				// 检查响应是否包含异常信息（从 AgentLlmNode 捕获的异常）
				if (message != null && message.getText() != null && message.getText().startsWith("Exception:")) {
					String exceptionText = message.getText();
					log.warn("模型调用返回异常消息: {}", exceptionText);
					
					// 从文本中提取异常信息并判断是否可重试
					if (attempt < maxAttempts && isRetryableExceptionMessage(exceptionText)) {
						lastException = new RuntimeException(exceptionText);
						// 等待后重试
						if (currentDelay > 0) {
							try {
								log.info("等待 {} ms 后重试", currentDelay);
								Thread.sleep(currentDelay);
							} catch (InterruptedException e) {
								Thread.currentThread().interrupt();
								throw new RuntimeException("重试被中断", e);
							}
						}
						// 计算下次延迟时间（指数退避）
						currentDelay = Math.min((long) (currentDelay * backoffMultiplier), maxDelay);
						continue;
					} else if (attempt >= maxAttempts) {
						log.error("达到最大重试次数 {}，模型调用失败", maxAttempts);
						throw new RuntimeException("模型调用失败，已达到最大重试次数: " + exceptionText);
					}
					
					// 不可重试的异常，直接返回
					return modelResponse;
				}

				// 成功响应
				if (attempt > 1) {
					log.info("模型调用在第 {} 次尝试后成功", attempt);
				}
				return modelResponse;

			} catch (Exception e) {
				lastException = e;
				log.warn("模型调用失败（尝试 {}/{}）: {}", attempt, maxAttempts, e.getMessage());

				if (attempt >= maxAttempts) {
					log.error("达到最大重试次数 {}，模型调用失败", maxAttempts);
					throw new RuntimeException("模型调用失败，已达到最大重试次数", lastException);
				}

				// 判断异常是否可重试
				if (!retryableExceptionPredicate.test(e)) {
					log.warn("异常不可重试，直接抛出: {}", e.getMessage());
					throw new RuntimeException("模型调用失败（不可重试的异常）", e);
				}

				// 等待后重试
				if (currentDelay > 0) {
					try {
						log.info("等待 {} ms 后重试", currentDelay);
						Thread.sleep(currentDelay);
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						throw new RuntimeException("重试被中断", ie);
					}
				}

				// 计算下次延迟时间（指数退避）
				currentDelay = Math.min((long) (currentDelay * backoffMultiplier), maxDelay);
			}
		}

		// 所有重试都失败
		throw new RuntimeException("模型调用失败，已达到最大重试次数 " + maxAttempts, lastException);
	}

	/**
	 * 判断异常消息是否表示可重试的错误
	 */
	private boolean isRetryableExceptionMessage(String exceptionText) {
		String lowerText = exceptionText.toLowerCase();
		return lowerText.contains("i/o error") ||
				lowerText.contains("remote host terminated") ||
				lowerText.contains("connection") ||
				lowerText.contains("timeout") ||
				lowerText.contains("network") ||
				lowerText.contains("handshake") ||
				lowerText.contains("socket");
	}

	@Override
	public String getName() {
		return "ModelRetry";
	}

	public static class Builder {
		private int maxAttempts = 3;
		private long initialDelay = 1000; // 1秒
		private long maxDelay = 30000; // 30秒
		private double backoffMultiplier = 2.0;
		private Predicate<Exception> retryableExceptionPredicate = Builder::isRetryableException;

		/**
		 * 设置最大重试次数（包含首次调用）
		 * @param maxAttempts 最大尝试次数，必须 >= 1
		 */
		public Builder maxAttempts(int maxAttempts) {
			if (maxAttempts < 1) {
				throw new IllegalArgumentException("maxAttempts 必须 >= 1");
			}
			this.maxAttempts = maxAttempts;
			return this;
		}

		/**
		 * 设置初始重试延迟（毫秒）
		 * @param initialDelay 初始延迟时间，单位毫秒
		 */
		public Builder initialDelay(long initialDelay) {
			if (initialDelay < 0) {
				throw new IllegalArgumentException("initialDelay 必须 >= 0");
			}
			this.initialDelay = initialDelay;
			return this;
		}

		/**
		 * 设置最大重试延迟（毫秒）
		 * @param maxDelay 最大延迟时间，单位毫秒
		 */
		public Builder maxDelay(long maxDelay) {
			if (maxDelay < 0) {
				throw new IllegalArgumentException("maxDelay 必须 >= 0");
			}
			this.maxDelay = maxDelay;
			return this;
		}

		/**
		 * 设置退避倍数（每次重试延迟时间的倍增系数）
		 * @param backoffMultiplier 退避倍数，必须 >= 1.0
		 */
		public Builder backoffMultiplier(double backoffMultiplier) {
			if (backoffMultiplier < 1.0) {
				throw new IllegalArgumentException("backoffMultiplier 必须 >= 1.0");
			}
			this.backoffMultiplier = backoffMultiplier;
			return this;
		}

		/**
		 * 设置自定义的可重试异常判断逻辑
		 * @param predicate 异常判断函数
		 */
		public Builder retryableExceptionPredicate(Predicate<Exception> predicate) {
			this.retryableExceptionPredicate = predicate;
			return this;
		}

		public ModelRetryInterceptor build() {
			return new ModelRetryInterceptor(this);
		}

		/**
		 * 默认的可重试异常判断逻辑
		 */
		private static boolean isRetryableException(Exception e) {
			String message = e.getMessage();
			if (message == null) {
				return false;
			}

			String lowerMessage = message.toLowerCase();
			
			// 网络相关异常
			if (lowerMessage.contains("i/o error") ||
				lowerMessage.contains("remote host terminated") ||
				lowerMessage.contains("connection") ||
				lowerMessage.contains("timeout") ||
				lowerMessage.contains("handshake") ||
				lowerMessage.contains("socket")) {
				return true;
			}

			// Spring WebClient 相关异常
			if (e.getClass().getName().contains("ResourceAccessException") ||
				e.getClass().getName().contains("WebClientRequestException")) {
				return true;
			}

			// 检查异常类型
			Throwable cause = e.getCause();
			while (cause != null) {
				String causeClassName = cause.getClass().getName();
				if (causeClassName.contains("IOException") ||
					causeClassName.contains("SocketException") ||
					causeClassName.contains("ConnectException") ||
					causeClassName.contains("TimeoutException") ||
					causeClassName.contains("SSLException")) {
					return true;
				}
				cause = cause.getCause();
			}

			return false;
		}
	}
}


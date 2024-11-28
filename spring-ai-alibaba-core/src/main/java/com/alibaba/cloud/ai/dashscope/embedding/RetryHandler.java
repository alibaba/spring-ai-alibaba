package com.alibaba.cloud.ai.dashscope.embedding;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryHandler {

	private static final Logger logger = LoggerFactory.getLogger(RetryHandler.class);

	private final int maxAttempts;

	private final long delayBetweenAttempts;

	public RetryHandler(int maxAttempts, long delayBetweenAttempts) {
		if (maxAttempts <= 0) {
			throw new IllegalArgumentException("maxAttempts must be greater than 0");
		}
		this.maxAttempts = maxAttempts;
		this.delayBetweenAttempts = delayBetweenAttempts;
	}

	public <T> T executeWithRetry(RetryableAction<T> action, String errorMessage) {
		int attempt = 0;
		while (true) {
			try {
				attempt++;
				return action.execute(); // Execute the action
			}
			catch (Exception e) {
				logger.error("Attempt {} failed: {}", attempt, e.getMessage());
				if (attempt >= maxAttempts) {
					logger.error("{}: Max retry attempts reached.", errorMessage);
					throw new RuntimeException(errorMessage, e);
				}
				try {
					Thread.sleep(delayBetweenAttempts); // Add delay between attempts
				}
				catch (InterruptedException ie) {
					Thread.currentThread().interrupt();
					throw new RuntimeException("Retry interrupted", ie);
				}
			}
		}
	}

	@FunctionalInterface
	public interface RetryableAction<T> {

		T execute() throws Exception;

	}

}

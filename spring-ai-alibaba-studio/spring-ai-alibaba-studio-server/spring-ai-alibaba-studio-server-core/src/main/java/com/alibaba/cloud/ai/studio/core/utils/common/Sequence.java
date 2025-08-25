/*
 * Copyright 2025 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.utils.common;

import jodd.util.StringPool;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * A distributed unique ID generator based on Twitter's Snowflake algorithm. Generates
 * 64-bit unique IDs using timestamp, datacenter ID, worker ID and sequence number.
 *
 * @since 1.0.0.3
 */
public class Sequence {

	/**
	 * Maximum initialization time interval in nanoseconds
	 */
	public static long MAX_START_INTERVAL_TIME = TimeUnit.SECONDS.toNanos(5);

	private static final Logger logger = LoggerFactory.getLogger(Sequence.class);

	/**
	 * Starting timestamp (2010-11-04 09:42:54.657)
	 */
	private static final long twepoch = 1288834974657L;

	/**
	 * Number of bits for worker ID
	 */
	private final long workerIdBits = 5L;

	/**
	 * Number of bits for datacenter ID
	 */
	private final long datacenterIdBits = 5L;

	private final long maxWorkerId = ~(-1L << workerIdBits);

	private final long maxDatacenterId = ~(-1L << datacenterIdBits);

	/**
	 * Number of bits for sequence number
	 */
	private final long sequenceBits = 12L;

	/**
	 * Worker ID
	 */
	private final long workerId;

	/**
	 * Datacenter ID
	 */
	private final long datacenterId;

	/**
	 * Sequence number for concurrent control
	 */
	private long sequence = 0L;

	/**
	 * Last timestamp when ID was generated
	 */
	private long lastTimestamp = -1L;

	/**
	 * IP address
	 */
	private InetAddress inetAddress;

	public Sequence() {
		this(null);
	}

	public Sequence(InetAddress inetAddress) {
		this.inetAddress = inetAddress;
		long start = System.nanoTime();
		this.datacenterId = getDatacenterId(maxDatacenterId);
		this.workerId = getMaxWorkerId(datacenterId, maxWorkerId);
		long end = System.nanoTime();
		if (end - start > Sequence.MAX_START_INTERVAL_TIME) {
			// 一般这里启动慢,是未指定inetAddress时出现,请查看本机hostname,将本机hostname写入至本地系统hosts文件之中进行解析
			logger.warn("Initialization Sequence Very Slow! Get datacenterId:{} workerId:{}", this.datacenterId,
					this.workerId);
		}
		else {
			initLog();
		}
	}

	private void initLog() {
		if (logger.isDebugEnabled()) {
			logger.debug("Initialization Sequence datacenterId:{} workerId:{}", this.datacenterId, this.workerId);
		}
	}

	/**
	 * 有参构造器
	 * @param workerId 工作机器 ID
	 * @param datacenterId 序列号
	 */
	public Sequence(long workerId, long datacenterId) {
		Assert.isTrue(!(workerId > maxWorkerId || workerId < 0),
				String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
		Assert.isTrue(!(datacenterId > maxDatacenterId || datacenterId < 0),
				String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
		this.workerId = workerId;
		this.datacenterId = datacenterId;
		initLog();
	}

	/**
	 * 获取 maxWorkerId
	 */
	protected long getMaxWorkerId(long datacenterId, long maxWorkerId) {
		StringBuilder mpid = new StringBuilder();
		mpid.append(datacenterId);
		String name = ManagementFactory.getRuntimeMXBean().getName();
		if (StringUtils.isNotBlank(name)) {
			/*
			 * GET jvmPid
			 */
			mpid.append(name.split(StringPool.AT)[0]);
		}
		/*
		 * MAC + PID 的 hashcode 获取16个低位
		 */
		return (mpid.toString().hashCode() & 0xffff) % (maxWorkerId + 1);
	}

	/**
	 * 数据标识id部分
	 */
	protected long getDatacenterId(long maxDatacenterId) {
		long id = 0L;
		try {
			if (null == this.inetAddress) {
				if (logger.isDebugEnabled()) {
					logger.debug("Use localhost address ");
				}
				this.inetAddress = InetAddress.getLocalHost();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Get {} network interface ", inetAddress);
			}
			NetworkInterface network = NetworkInterface.getByInetAddress(this.inetAddress);
			if (logger.isDebugEnabled()) {
				logger.debug("Get network interface info: {}", network);
			}
			if (null == network) {
				logger.warn("Unable to get network interface for {}", inetAddress);
				id = 1L;
			}
			else {
				byte[] mac = network.getHardwareAddress();
				if (null != mac) {
					id = ((0x000000FF & (long) mac[mac.length - 2])
							| (0x0000FF00 & (((long) mac[mac.length - 1]) << 8))) >> 6;
					id = id % (maxDatacenterId + 1);
				}
			}
		}
		catch (Exception e) {
			logger.warn(" getDatacenterId: {}", e.getMessage());
		}
		return id;
	}

	/**
	 * Get the next unique ID
	 * @return next unique ID
	 */
	public synchronized long nextId() {
		long timestamp = timeGen();
		// Handle clock moving backwards
		if (timestamp < lastTimestamp) {
			long offset = lastTimestamp - timestamp;
			if (offset <= 5) {
				try {
					wait(offset << 1);
					timestamp = timeGen();
					if (timestamp < lastTimestamp) {
						throw new RuntimeException(String
							.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
					}
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
			else {
				throw new RuntimeException(
						String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", offset));
			}
		}

		if (lastTimestamp == timestamp) {
			// Increment sequence number within the same millisecond
			long sequenceMask = ~(-1L << sequenceBits);
			sequence = (sequence + 1) & sequenceMask;
			if (sequence == 0) {
				// Sequence number reached maximum, wait for next millisecond
				timestamp = tilNextMillis(lastTimestamp);
			}
		}
		else {
			// Different millisecond, reset sequence to random number between 1-2
			sequence = ThreadLocalRandom.current().nextLong(1, 3);
		}

		lastTimestamp = timestamp;

		// Combine timestamp, datacenter ID, worker ID and sequence number
		long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
		long datacenterIdShift = sequenceBits + workerIdBits;
		return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift)
				| (workerId << sequenceBits) | sequence;
	}

	/**
	 * Wait until next millisecond
	 */
	protected long tilNextMillis(long lastTimestamp) {
		long timestamp = timeGen();
		while (timestamp <= lastTimestamp) {
			timestamp = timeGen();
		}
		return timestamp;
	}

	/**
	 * Get current timestamp
	 */
	protected long timeGen() {
		return SystemClock.now();
	}

	/**
	 * Extract timestamp from ID
	 */
	public static long parseIdTimestamp(long id) {
		return (id >> 22) + twepoch;
	}

}

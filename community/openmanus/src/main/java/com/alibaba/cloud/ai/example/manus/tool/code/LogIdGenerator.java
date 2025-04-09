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

package com.alibaba.cloud.ai.example.manus.tool.code;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;

import com.google.common.net.InetAddresses;

import cn.hutool.core.util.ByteUtil;

public class LogIdGenerator {

	private static final int VERSION_LEN = Short.BYTES;

	private static final int TIME_LEN = Long.BYTES;

	private static final int IP_LEN = Integer.BYTES;

	private static final int SERIAL_LEN = Integer.BYTES;

	/**
	 * 总字节数一定要是3的倍数，确保base64编码不用padding，否则会在编码结果中引入特殊符号=
	 */
	private static final int TOTAL_LEN = VERSION_LEN + TIME_LEN + IP_LEN + SERIAL_LEN;

	private static final short CUR_VERSION = 0;

	private static final int CUR_IP = InetAddresses.coerceToInteger(InetAddresses.forString(IpUtils.getLocalIp()));

	private static final AtomicInteger SERIAL_GEN = new AtomicInteger(Integer.MIN_VALUE);

	/**
	 * 生成一个uuid 这里采用雪花算法的思路，用版本(2byte)+时间戳(8byte)+IP(4byte)+序列号(4byte)的方式生成一个全局大致有序的uuid
	 * 然后用base64进行编码，使得最后生成的字符串只包含[0-9a-zA-Z]-_，不含其他任何特殊符号
	 */
	public static String generateUniqueId() {
		byte[] array = new byte[TOTAL_LEN];

		int countLength = 0;

		byte[] versionByte = ByteUtil.shortToBytes(CUR_VERSION);
		System.arraycopy(versionByte, 0, array, countLength, versionByte.length);
		countLength += versionByte.length;

		byte[] timeByte = ByteUtil.longToBytes(System.currentTimeMillis());
		System.arraycopy(timeByte, 0, array, countLength, timeByte.length);
		countLength += timeByte.length;

		byte[] ipByte = ByteUtil.intToBytes(CUR_IP);
		System.arraycopy(ipByte, 0, array, countLength, ipByte.length);
		countLength += ipByte.length;

		byte[] serialByte = ByteUtil.intToBytes(SERIAL_GEN.getAndIncrement());
		System.arraycopy(serialByte, 0, array, countLength, serialByte.length);

		return new String(Base64.getUrlEncoder().encode(array), StandardCharsets.UTF_8);
	}

	public static final String DEFAULT_INIT_AIB_RPC_ID = "0.1";

	private static final String format = "%s.%s";

	/**
	 * 通过uniaue id 和对应的increment id 生成 aibRpcId，注意并发情况下的生成
	 * @param aibRpcId aibRpcId
	 * @param incrementId incrementId
	 * @return new aibRpcId
	 */
	public static String generateSubAibRpcId(String aibRpcId, AtomicLong incrementId) {
		// 为空则生成默认初始rpcid
		if (StringUtils.isEmpty(aibRpcId)) {
			return DEFAULT_INIT_AIB_RPC_ID;
		}
		return String.format(format, aibRpcId, incrementId.getAndIncrement());
	}

	/**
	 * 默认向下加载一层
	 * @param aibRpcId aibRpcId
	 * @return new aibRpcId
	 */
	public static String generateSubAibRpcId(String aibRpcId) {
		// 为空则生成默认初始rpcid
		if (StringUtils.isEmpty(aibRpcId)) {
			return DEFAULT_INIT_AIB_RPC_ID;
		}
		return String.format(format, aibRpcId, "1");
	}

	/**
	 * 默认向下加载一层
	 * @param aibRpcId aibRpcId
	 * @return new aibRpcId
	 */
	public static String generateSubAibRpcId(String aibRpcId, Long subRpcIdIndex) {
		// 为空则生成默认初始rpcid
		if (StringUtils.isEmpty(aibRpcId)) {
			return DEFAULT_INIT_AIB_RPC_ID;
		}
		if (Objects.isNull(subRpcIdIndex)) {
			return String.format(format, aibRpcId, "1");
		}
		return String.format(format, aibRpcId, subRpcIdIndex);
	}

}

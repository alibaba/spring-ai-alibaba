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

package com.alibaba.cloud.ai.manus.tool.code;

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
	 * Total bytes must be a multiple of 3 to ensure base64 encoding doesn't need padding,
	 * otherwise special symbols = will be introduced in the encoded result
	 */
	private static final int TOTAL_LEN = VERSION_LEN + TIME_LEN + IP_LEN + SERIAL_LEN;

	private static final short CUR_VERSION = 0;

	private static final int CUR_IP = InetAddresses.coerceToInteger(InetAddresses.forString(IpUtils.getLocalIp()));

	private static final AtomicInteger SERIAL_GEN = new AtomicInteger(Integer.MIN_VALUE);

	/**
	 * Generate a UUID using snowflake algorithm approach, generating a globally roughly
	 * ordered UUID with version(2byte)+timestamp(8byte)+IP(4byte)+serial number(4byte)
	 * Then use base64 encoding to make the final generated string only contain
	 * [0-9a-zA-Z]-_, without any other special symbols
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
	 * Generate aibRpcId through unique id and corresponding increment id, note generation
	 * under concurrent situations
	 * @param aibRpcId aibRpcId
	 * @param incrementId incrementId
	 * @return new aibRpcId
	 */
	public static String generateSubAibRpcId(String aibRpcId, AtomicLong incrementId) {
		// If empty, generate default initial rpcid
		if (StringUtils.isEmpty(aibRpcId)) {
			return DEFAULT_INIT_AIB_RPC_ID;
		}
		return String.format(format, aibRpcId, incrementId.getAndIncrement());
	}

	/**
	 * Default load one level down
	 * @param aibRpcId aibRpcId
	 * @return new aibRpcId
	 */
	public static String generateSubAibRpcId(String aibRpcId) {
		// If empty, generate default initial rpcid
		if (StringUtils.isEmpty(aibRpcId)) {
			return DEFAULT_INIT_AIB_RPC_ID;
		}
		return String.format(format, aibRpcId, "1");
	}

	/**
	 * Default load one level down
	 * @param aibRpcId aibRpcId
	 * @return new aibRpcId
	 */
	public static String generateSubAibRpcId(String aibRpcId, Long subRpcIdIndex) {
		// If empty, generate default initial rpcid
		if (StringUtils.isEmpty(aibRpcId)) {
			return DEFAULT_INIT_AIB_RPC_ID;
		}
		if (Objects.isNull(subRpcIdIndex)) {
			return String.format(format, aibRpcId, "1");
		}
		return String.format(format, aibRpcId, subRpcIdIndex);
	}

}

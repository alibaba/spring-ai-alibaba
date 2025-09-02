/*
 * Copyright 2024-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.a2a.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author xiweng.yy
 */
public class InetUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(InetUtils.class);

	public static InetAddress findFirstNonLoopbackIpv4Address() {
		return findFirstNonLoopbackAddress(inetAddress -> inetAddress instanceof Inet4Address);
	}

	public static InetAddress findFirstNonLoopbackIpv6Address() {
		return findFirstNonLoopbackAddress(inetAddress -> inetAddress instanceof Inet6Address);
	}

	private static InetAddress findFirstNonLoopbackAddress(Function<InetAddress, Boolean> typeFilter) {
		InetAddress result = null;
		try {
			int lowest = Integer.MAX_VALUE;
			for (Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces(); nics
				.hasMoreElements();) {
				NetworkInterface ifc = nics.nextElement();
				if (ifc.isUp()) {
					LOGGER.trace("Testing interface: " + ifc.getDisplayName());
					if (ifc.getIndex() < lowest || result == null) {
						lowest = ifc.getIndex();
					}
					else if (result != null) {
						continue;
					}

					for (Enumeration<InetAddress> addrs = ifc.getInetAddresses(); addrs.hasMoreElements();) {
						InetAddress address = addrs.nextElement();
						if (typeFilter.apply(address) && !address.isLoopbackAddress()) {
							LOGGER.trace("Found non-loopback interface: " + ifc.getDisplayName());
							result = address;
						}
					}
				}
			}
		}
		catch (IOException ex) {
			LOGGER.error("Cannot get first non-loopback address", ex);
		}

		if (result != null) {
			return result;
		}

		try {
			return InetAddress.getLocalHost();
		}
		catch (UnknownHostException e) {
			LOGGER.warn("Unable to retrieve localhost");
		}

		return null;
	}

}

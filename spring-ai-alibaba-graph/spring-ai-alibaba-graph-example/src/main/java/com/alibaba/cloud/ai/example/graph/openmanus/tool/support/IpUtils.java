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

package com.alibaba.cloud.ai.example.graph.openmanus.tool.support;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

public class IpUtils {

	private static String LOCAL_IP = "127.0.0.1";

	public IpUtils() {
	}

	public static String getLocalIp() {
		return LOCAL_IP;
	}

	static {
		try {
			Enumeration<NetworkInterface> nis = NetworkInterface.getNetworkInterfaces();

			label38: while (true) {
				NetworkInterface ni;
				do {
					do {
						if (!nis.hasMoreElements()) {
							break label38;
						}

						ni = (NetworkInterface) nis.nextElement();
					}
					while (ni.isLoopback());
				}
				while (ni.isVirtual());

				Enumeration<InetAddress> addresss = ni.getInetAddresses();

				while (addresss.hasMoreElements()) {
					InetAddress address = (InetAddress) addresss.nextElement();
					if (address instanceof Inet4Address) {
						LOCAL_IP = address.getHostAddress();
					}
				}
			}
		}
		catch (Throwable var4) {
		}
	}

}

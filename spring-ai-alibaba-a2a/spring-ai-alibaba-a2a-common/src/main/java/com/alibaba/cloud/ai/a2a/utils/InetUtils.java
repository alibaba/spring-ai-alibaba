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

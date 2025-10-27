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
package com.alibaba.cloud.ai.graph.agent.hook.pii;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built-in PII detectors for common types.
 */
public class PIIDetectors {

	private static final Pattern EMAIL_PATTERN =
			Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");

	private static final Pattern CREDIT_CARD_PATTERN =
			Pattern.compile("\\b\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}[\\s-]?\\d{4}\\b");

	private static final Pattern IPV4_PATTERN =
			Pattern.compile("\\b(?:[0-9]{1,3}\\.){3}[0-9]{1,3}\\b");

	private static final Pattern MAC_ADDRESS_PATTERN =
			Pattern.compile("\\b([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})\\b");

	private static final Pattern URL_PATTERN =
			Pattern.compile("\\bhttps?://[\\w\\-]+(\\.[\\w\\-]+)+[/#?]?.*?\\b");

	public static PIIDetector emailDetector() {
		return content -> {
			List<PIIMatch> matches = new ArrayList<>();
			Matcher matcher = EMAIL_PATTERN.matcher(content);
			while (matcher.find()) {
				matches.add(new PIIMatch("email", matcher.group(),
						matcher.start(), matcher.end()));
			}
			return matches;
		};
	}

	public static PIIDetector creditCardDetector() {
		return content -> {
			List<PIIMatch> matches = new ArrayList<>();
			Matcher matcher = CREDIT_CARD_PATTERN.matcher(content);
			while (matcher.find()) {
				String cardNumber = matcher.group();
				if (passesLuhn(cardNumber)) {
					matches.add(new PIIMatch("credit_card", cardNumber,
							matcher.start(), matcher.end()));
				}
			}
			return matches;
		};
	}

	public static PIIDetector ipDetector() {
		return content -> {
			List<PIIMatch> matches = new ArrayList<>();
			Matcher matcher = IPV4_PATTERN.matcher(content);
			while (matcher.find()) {
				String ip = matcher.group();
				try {
					InetAddress.getByName(ip);
					matches.add(new PIIMatch("ip", ip, matcher.start(), matcher.end()));
				}
				catch (Exception e) {
					// Invalid IP, skip
				}
			}
			return matches;
		};
	}

	public static PIIDetector macAddressDetector() {
		return content -> {
			List<PIIMatch> matches = new ArrayList<>();
			Matcher matcher = MAC_ADDRESS_PATTERN.matcher(content);
			while (matcher.find()) {
				matches.add(new PIIMatch("mac_address", matcher.group(),
						matcher.start(), matcher.end()));
			}
			return matches;
		};
	}

	public static PIIDetector urlDetector() {
		return content -> {
			List<PIIMatch> matches = new ArrayList<>();
			Matcher matcher = URL_PATTERN.matcher(content);
			while (matcher.find()) {
				matches.add(new PIIMatch("url", matcher.group(),
						matcher.start(), matcher.end()));
			}
			return matches;
		};
	}

	public static PIIDetector regexDetector(String type, String pattern) {
		Pattern compiledPattern = Pattern.compile(pattern);
		return content -> {
			List<PIIMatch> matches = new ArrayList<>();
			Matcher matcher = compiledPattern.matcher(content);
			while (matcher.find()) {
				matches.add(new PIIMatch(type, matcher.group(),
						matcher.start(), matcher.end()));
			}
			return matches;
		};
	}

	/**
	 * Validates a credit card number using the Luhn algorithm.
	 */
	private static boolean passesLuhn(String cardNumber) {
		String digits = cardNumber.replaceAll("[\\s-]", "");
		if (digits.length() < 13 || digits.length() > 19) {
			return false;
		}

		int sum = 0;
		boolean alternate = false;
		for (int i = digits.length() - 1; i >= 0; i--) {
			int digit = Character.getNumericValue(digits.charAt(i));
			if (alternate) {
				digit *= 2;
				if (digit > 9) {
					digit -= 9;
				}
			}
			sum += digit;
			alternate = !alternate;
		}
		return (sum % 10) == 0;
	}
}


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
package com.alibaba.cloud.ai.toolcalling.crawler;

import java.util.regex.Pattern;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

public final class UrlValidator {

	private UrlValidator() {
	}

	private static final String URL_REGEX = "^(https?://)?"
			+ "((([a-zA-Z0-9\\-]+\\.)+[a-zA-Z]{2,})|(([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})" + // 域名或
																											// IP
			"(?!(\\.0\\.0\\.0|\\.0\\.0|\\.0|\\.1|\\.2|\\.3|\\.4|\\.5|\\.6|\\.7|\\.8|\\.9))" + // 排除
																								// 127.0.0.1
			"))" + "(\\:[0-9]{1,5})?" + "(/.*)?$";

	private static final Pattern pattern = Pattern.compile(URL_REGEX);

	public static boolean isValidUrl(String target) {

		return pattern.matcher(target).matches();
	}

}

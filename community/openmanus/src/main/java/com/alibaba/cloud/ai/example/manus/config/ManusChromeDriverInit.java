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

package com.alibaba.cloud.ai.example.manus.config;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import com.alibaba.cloud.ai.example.manus.OpenManusSpringBootApplication;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Component
public class ManusChromeDriverInit implements ApplicationRunner {

	@Override
	public void run(ApplicationArguments args) throws Exception {

		String chromedriverPath;

		if (checkOS()) {
			chromedriverPath = getChromedriverPath("data/chromedriver.exe");
		}
		else {
			chromedriverPath = getChromedriverPath("data/chromedriver");
		}

		setChromeDriver(chromedriverPath);
	}

	private String getChromedriverPath(String resourcePath) throws URISyntaxException {

		URL resource = OpenManusSpringBootApplication.class.getClassLoader().getResource(resourcePath);
		if (resource == null) {
			throw new IllegalStateException("Chromedriver not found: " + resourcePath);
		}

		return Paths.get(resource.toURI()).toFile().getAbsolutePath();
	}

	private static Boolean checkOS() {

		String os = System.getProperty("os.name").toLowerCase();

		if (os.contains("win")) {
			return true;
		}
		else if (os.contains("mac")) {
			return false;
		}
		else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			System.out.println("Operating System: Unix/Linux");
			return false;
		}
		else {
			System.out.println("Operating System: Unknown");
			return false;
		}
	}

	private static void setChromeDriver(String path) {

		System.setProperty("webdriver.chrome.driver", path);
	}

}

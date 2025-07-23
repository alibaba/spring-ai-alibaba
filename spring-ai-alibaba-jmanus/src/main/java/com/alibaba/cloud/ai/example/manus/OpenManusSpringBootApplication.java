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

package com.alibaba.cloud.ai.example.manus;

import com.microsoft.playwright.Playwright;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class OpenManusSpringBootApplication {

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args != null && args.length >= 1 && args[0].equals("playwright-init")) {
			Playwright.create();
			System.out.println("Playwright init finished");
			System.exit(0);
		}
		else {
			SpringApplication.run(OpenManusSpringBootApplication.class, args);
		}
	}

}

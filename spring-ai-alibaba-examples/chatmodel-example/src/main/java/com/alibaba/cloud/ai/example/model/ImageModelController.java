/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alibaba.cloud.ai.example.model;

import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImageOptions;
import org.springframework.ai.image.ImageOptionsBuilder;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class ImageModelController {

	private final ImageModel imageModel;

	ImageModelController(ImageModel imageModel) {
		this.imageModel = imageModel;
	}

	@GetMapping("/image/{input}")
	public void image(@PathVariable("input") String input, HttpServletResponse response) {

		ImageOptions options = ImageOptionsBuilder.builder()
				.withModel("wanx-v1")
				.build();

		ImagePrompt imagePrompt = new ImagePrompt(input, options);
		ImageResponse imageResponse = imageModel.call(imagePrompt);
		String imageUrl = imageResponse.getResult().getOutput().getUrl();

		try {
			URL url = new URL(imageUrl);
			InputStream in = url.openStream();

			response.setHeader("Content-Type", MediaType.IMAGE_PNG_VALUE);
			response.getOutputStream().write(in.readAllBytes());
			response.getOutputStream().flush();
		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}

}

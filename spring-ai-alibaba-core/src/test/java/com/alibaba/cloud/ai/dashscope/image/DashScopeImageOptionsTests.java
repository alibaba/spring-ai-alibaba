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
package com.alibaba.cloud.ai.dashscope.image;

import org.junit.jupiter.api.Test;
import org.springframework.ai.image.ImageOptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases for DashScopeImageOptions. Tests cover builder pattern, getters/setters,
 * size calculations, and various edge cases.
 *
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 * @author brianxiadong
 * @since 1.0.0-M5.1
 */
class DashScopeImageOptionsTests {

	// Test constants
	private static final String TEST_MODEL = "wanx-v1";

	private static final Integer TEST_N = 2;

	private static final Integer TEST_WIDTH = 1024;

	private static final Integer TEST_HEIGHT = 720;

	private static final String TEST_STYLE = "photography";

	private static final Integer TEST_SEED = 42;

	private static final String TEST_REF_IMG = "https://example.com/image.jpg";

	private static final Float TEST_REF_STRENGTH = 0.8f;

	private static final String TEST_REF_MODE = "repaint";

	private static final String TEST_NEGATIVE_PROMPT = "blurry, low quality";

	@Test
	void testBuilderAndGetters() {
		// Test building DashScopeImageOptions using builder pattern and verify getters
		DashScopeImageOptions options = DashScopeImageOptions.builder()
			.withModel(TEST_MODEL)
			.withN(TEST_N)
			.withWidth(TEST_WIDTH)
			.withHeight(TEST_HEIGHT)
			.withStyle(TEST_STYLE)
			.withSeed(TEST_SEED)
			.withRefImg(TEST_REF_IMG)
			.withRefStrength(TEST_REF_STRENGTH)
			.withRefMode(TEST_REF_MODE)
			.withNegativePrompt(TEST_NEGATIVE_PROMPT)
			.build();

		// Verify all fields are set correctly
		assertThat(options.getModel()).isEqualTo(TEST_MODEL);
		assertThat(options.getN()).isEqualTo(TEST_N);
		assertThat(options.getWidth()).isEqualTo(TEST_WIDTH);
		assertThat(options.getHeight()).isEqualTo(TEST_HEIGHT);
		assertThat(options.getStyle()).isEqualTo(TEST_STYLE);
		assertThat(options.getSeed()).isEqualTo(TEST_SEED);
		assertThat(options.getRefImg()).isEqualTo(TEST_REF_IMG);
		assertThat(options.getRefStrength()).isEqualTo(TEST_REF_STRENGTH);
		assertThat(options.getRefMode()).isEqualTo(TEST_REF_MODE);
		assertThat(options.getNegativePrompt()).isEqualTo(TEST_NEGATIVE_PROMPT);
	}

	@Test
	void testSettersAndGetters() {
		// Test setters and getters
		DashScopeImageOptions options = new DashScopeImageOptions();

		options.setModel(TEST_MODEL);
		options.setN(TEST_N);
		options.setWidth(TEST_WIDTH);
		options.setHeight(TEST_HEIGHT);
		options.setStyle(TEST_STYLE);
		options.setSeed(TEST_SEED);
		options.setRefImg(TEST_REF_IMG);
		options.setRefStrength(TEST_REF_STRENGTH);
		options.setRefMode(TEST_REF_MODE);
		options.setNegativePrompt(TEST_NEGATIVE_PROMPT);

		// Verify all fields are set correctly
		assertThat(options.getModel()).isEqualTo(TEST_MODEL);
		assertThat(options.getN()).isEqualTo(TEST_N);
		assertThat(options.getWidth()).isEqualTo(TEST_WIDTH);
		assertThat(options.getHeight()).isEqualTo(TEST_HEIGHT);
		assertThat(options.getStyle()).isEqualTo(TEST_STYLE);
		assertThat(options.getSeed()).isEqualTo(TEST_SEED);
		assertThat(options.getRefImg()).isEqualTo(TEST_REF_IMG);
		assertThat(options.getRefStrength()).isEqualTo(TEST_REF_STRENGTH);
		assertThat(options.getRefMode()).isEqualTo(TEST_REF_MODE);
		assertThat(options.getNegativePrompt()).isEqualTo(TEST_NEGATIVE_PROMPT);
	}

	@Test
	void testDefaultValues() {
		// Test default values when creating a new instance
		DashScopeImageOptions options = new DashScopeImageOptions();

		// Verify default values are null
		assertThat(options.getModel()).isNull();
		assertThat(options.getN()).isNull();
		assertThat(options.getWidth()).isNull();
		assertThat(options.getHeight()).isNull();
		assertThat(options.getStyle()).isNull();
		assertThat(options.getSeed()).isNull();
		assertThat(options.getRefImg()).isNull();
		assertThat(options.getRefStrength()).isNull();
		assertThat(options.getRefMode()).isNull();
		assertThat(options.getNegativePrompt()).isNull();
	}

	@Test
	void testSizeCalculation() {
		// Test size calculation with width and height
		DashScopeImageOptions options = DashScopeImageOptions.builder()
			.withWidth(TEST_WIDTH)
			.withHeight(TEST_HEIGHT)
			.build();

		// Verify size is calculated correctly
		assertThat(options.getSize()).isEqualTo(TEST_WIDTH + "*" + TEST_HEIGHT);
	}

	@Test
	void testSizeWithDirectSetting() {
		// Test setting size directly
		DashScopeImageOptions options = new DashScopeImageOptions();
		String size = "1280*720";
		options.setSize(size);

		// Verify size is set correctly
		assertThat(options.getSize()).isEqualTo(size);
	}

	@Test
	void testSizeWithNullDimensions() {
		// Test size calculation with null dimensions
		DashScopeImageOptions options = new DashScopeImageOptions();

		// Verify size is null when dimensions are null
		assertThat(options.getSize()).isNull();
	}

	@Test
	void testImplementsImageOptions() {
		// Test that DashScopeImageOptions implements ImageOptions interface
		DashScopeImageOptions options = new DashScopeImageOptions();

		assertThat(options).isInstanceOf(ImageOptions.class);
	}

	@Test
	void testToString() {
		// Test toString method
		DashScopeImageOptions options = DashScopeImageOptions.builder()
			.withModel(TEST_MODEL)
			.withN(TEST_N)
			.withWidth(TEST_WIDTH)
			.withHeight(TEST_HEIGHT)
			.build();

		String toString = options.toString();

		// Verify toString contains essential information
		assertThat(toString).contains("DashScopeImageOptions")
			.contains("model='" + TEST_MODEL + "'")
			.contains("n=" + TEST_N)
			.contains("width=" + TEST_WIDTH)
			.contains("height=" + TEST_HEIGHT);
	}

}

/*
* Copyright 2024 the original author or authors.
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

package com.alibaba.cloud.ai.studio.core.crypto;

import com.alibaba.cloud.ai.studio.core.utils.security.RSACryptUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for RSA encryption and decryption functionality. Tests the implementation of
 * RSA cryptographic operations.
 *
 * @version 1.0.0
 * @since jdk8
 */
class RSACryptTest {

	/**
	 * Tests the RSA encryption functionality. Verifies that data can be encrypted using
	 * RSA algorithm.
	 */
	@Test
	void encrypt() {
		String original = "This-is-a-test-message";
		String encrypted = RSACryptUtils.encrypt(original);
		assertNotNull(encrypted);
	}

	/**
	 * Tests the RSA decryption functionality. Verifies that encrypted data can be
	 * decrypted back to original form.
	 */
	@Test
	void decrypt() {
		String original = "This-is-a-test-message";
		String encrypted = RSACryptUtils.encrypt(original);
		String decrypted = RSACryptUtils.decrypt(encrypted);
		assertEquals(original, decrypted);
	}

}

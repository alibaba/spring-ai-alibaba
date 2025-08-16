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

import com.alibaba.cloud.ai.studio.core.utils.security.PasswordCryptUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Title Unit test cases for PasswordCrypt.<br/>
 * Description Unit test cases for PasswordCrypt.<br/>
 * Created at 2025-05-22 20:37
 *

 * @version 1.0.0
 * @since jdk8
 */

/**
 * Test class for PasswordCrypt service. Tests password encoding and matching
 * functionality.
 */
class PasswordCryptTest {

	/**
	 * Tests password encoding functionality. Verifies that a password can be successfully
	 * encoded.
	 */
	@Test
	void encode() {
		assertNotNull(PasswordCryptUtils.encode("123456"));
	}

	/**
	 * Tests password matching functionality. Verifies that a password matches its encoded
	 * version.
	 */
	@Test
	void match() {
		String password = "123456";
		assertTrue(PasswordCryptUtils.match(password, PasswordCryptUtils.encode(password)));
	}

}

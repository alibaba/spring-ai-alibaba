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

package com.alibaba.cloud.ai.studio.core.utils.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Password encryption utility using Argon2id algorithm. Provides methods for password
 * hashing and verification.
 *
 * @since 1.0.0.3
 */
public class PasswordCryptUtils {

	/** Number of iterations for password hashing */
	private static final int ITERATIONS = 2;

	/** Memory cost in KB */
	private static final int MEMORY = 66536;

	/** Length of the generated hash in bytes */
	private static final int HASH_LENGTH = 32;

	/** Number of parallel threads */
	private static final int PARALLELISM = 1;

	/** Base64 encoder without padding */
	private static final Base64.Encoder b64encoder = Base64.getEncoder().withoutPadding();

	/** Base64 decoder */
	private static final Base64.Decoder b64decoder = Base64.getDecoder();

	/**
	 * Encodes a password using Argon2id algorithm.
	 * @param password The password to encode
	 * @return Encoded password string in Argon2id format
	 */
	public static String encode(String password) {
		byte[] salt = genSalt();
		byte[] hash = new byte[HASH_LENGTH];
		Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id).withSalt(salt)
			.withParallelism(PARALLELISM)
			.withMemoryAsKB(MEMORY)
			.withIterations(ITERATIONS)
			.build();
		Argon2BytesGenerator generator = new Argon2BytesGenerator();
		generator.init(params);
		generator.generateBytes(password.toCharArray(), hash);

		StringBuilder stringBuilder = new StringBuilder("$argon2id");
		stringBuilder.append("$v=")
			.append(params.getVersion())
			.append("$m=")
			.append(params.getMemory())
			.append(",t=")
			.append(params.getIterations())
			.append(",p=")
			.append(params.getLanes())
			.append("$")
			.append(b64encoder.encodeToString(salt))
			.append("$")
			.append(b64encoder.encodeToString(hash));
		return stringBuilder.toString();
	}

	/**
	 * Verifies if a password matches the encoded password.
	 * @param password The password to verify
	 * @param encodedPassword The encoded password to check against
	 * @return true if the password matches, false otherwise
	 * @throws IllegalArgumentException if the encoded password format is invalid
	 */
	public static boolean match(String password, String encodedPassword) {
		String[] parts = encodedPassword.split("\\$");
		if (parts.length < 4) {
			throw new IllegalArgumentException("Invalid encoded Argon2-hash");
		}

		Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id);

		if (parts[2].startsWith("$v=")) {
			int version = Integer.parseInt(parts[0].substring(2));
			builder.withVersion(version);
		}

		String[] perfParams = parts[3].split(",");
		if (perfParams.length != 3) {
			throw new IllegalArgumentException("Amount of performance parameters invalid");
		}

		if (!perfParams[0].startsWith("m=")) {
			throw new IllegalArgumentException("Invalid memory parameter");
		}
		builder.withMemoryAsKB(Integer.parseInt(perfParams[0].substring(2)));
		if (!perfParams[1].startsWith("t=")) {
			throw new IllegalArgumentException("Invalid iterations parameter");
		}
		builder.withIterations(Integer.parseInt(perfParams[1].substring(2)));
		if (!perfParams[2].startsWith("p=")) {
			throw new IllegalArgumentException("Invalid parallel parameter");
		}
		builder.withParallelism(Integer.parseInt(perfParams[2].substring(2)));

		builder.withSalt(b64decoder.decode(parts[4]));

		byte[] decoded = b64decoder.decode(parts[5]);
		byte[] hashBytes = new byte[decoded.length];

		Argon2BytesGenerator generator = new Argon2BytesGenerator();
		generator.init(builder.build());
		generator.generateBytes(password.toCharArray(), hashBytes);

		int result = 0;
		for (int i = 0; i < decoded.length; i++) {
			result |= decoded[i] ^ hashBytes[i];
		}
		return result == 0;
	}

	/**
	 * Generates a random salt for password hashing.
	 * @return 16-byte random salt
	 */
	private static byte[] genSalt() {
		SecureRandom secureRandom = new SecureRandom();
		byte[] salt = new byte[16];
		secureRandom.nextBytes(salt);

		return salt;
	}

}

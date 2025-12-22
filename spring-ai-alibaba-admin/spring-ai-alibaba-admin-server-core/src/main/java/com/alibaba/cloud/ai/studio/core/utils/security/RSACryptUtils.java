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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * Utility class for RSA encryption and decryption operations.
 *
 * @since 1.0.0.3
 */
public class RSACryptUtils {

	/** RSA algorithm name */
	private static final String RSA_ALGORITHM = "RSA";

	/** RSA OAEP algorithm with SHA-256 and MGF1 padding */
	private static final String RSA_OAEP_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

	/** Public key loaded from resource */
	private static final String publicKey;

	/** Private key loaded from resource */
	private static final String privateKey;

	static {
		try {
			KeyPair keyPair = RSACryptUtils.loadKeyPair(new ClassPathResource("keys/private.pem"),
					new ClassPathResource("keys/public.pem"));
			publicKey = RSACryptUtils.keyToString(keyPair.getPublic());
			privateKey = RSACryptUtils.keyToString(keyPair.getPrivate());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Encrypts the given string using the default public key.
	 * @param original String to encrypt
	 * @return Encrypted string in Base64 format
	 */
	public static String encrypt(String original) {
		try {
			return encrypt(original, publicKey);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Decrypts the given encrypted string using the default private key.
	 * @param encrypted Encrypted string in Base64 format
	 * @return Decrypted string
	 */
	public static String decrypt(String encrypted) {
		try {
			return decrypt(encrypted, privateKey);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Generates a new RSA key pair.
	 * @param keySize Key size in bits (e.g., 2048)
	 * @return KeyPair containing public and private keys
	 * @throws NoSuchAlgorithmException if RSA algorithm is not available
	 */
	public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
		keyPairGenerator.initialize(keySize);
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Loads RSA key pair from PEM files.
	 * @param privateKeyResource Private key resource
	 * @param publicKeyResource Public key resource
	 * @return KeyPair loaded from the resources
	 * @throws Exception if key loading fails
	 */
	public static KeyPair loadKeyPair(Resource privateKeyResource, Resource publicKeyResource) throws Exception {
		// load private key
		byte[] privateKeyBytes = privateKeyResource.getContentAsByteArray();
		String privateKeyPEM = new String(privateKeyBytes).replace("-----BEGIN PRIVATE KEY-----", "")
			.replace("-----END PRIVATE KEY-----", "")
			.replaceAll("\\s+", "");
		byte[] decodedPrivateKey = Base64.getDecoder().decode(privateKeyPEM);
		PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(decodedPrivateKey);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);

		// load public key
		byte[] publicKeyBytes = publicKeyResource.getContentAsByteArray();
		String publicKeyPEM = new String(publicKeyBytes).replace("-----BEGIN PUBLIC KEY-----", "")
			.replace("-----END PUBLIC KEY-----", "")
			.replaceAll("\\s+", "");
		byte[] decodedPublicKey = Base64.getDecoder().decode(publicKeyPEM);
		X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(decodedPublicKey);
		PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);

		return new KeyPair(publicKey, privateKey);
	}

	/**
	 * Encrypts data using RSA OAEP with the specified public key.
	 * @param data Data to encrypt
	 * @param publicKey Public key in Base64 format
	 * @return Encrypted data in Base64 format
	 * @throws Exception if encryption fails
	 */
	public static String encrypt(String data, String publicKey) throws Exception {
		byte[] publicKeyBytes = Base64.getDecoder().decode(publicKey);
		X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
		PublicKey key = keyFactory.generatePublic(keySpec);

		Cipher cipher = Cipher.getInstance(RSA_OAEP_ALGORITHM);
		OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
				PSource.PSpecified.DEFAULT);
		cipher.init(Cipher.ENCRYPT_MODE, key, oaepParams);

		byte[] encryptedBytes = cipher.doFinal(data.getBytes());
		return Base64.getEncoder().encodeToString(encryptedBytes);
	}

	/**
	 * Decrypts data using RSA OAEP with the specified private key.
	 * @param encryptedData Encrypted data in Base64 format
	 * @param privateKey Private key in Base64 format
	 * @return Decrypted data
	 * @throws Exception if decryption fails
	 */
	public static String decrypt(String encryptedData, String privateKey) throws Exception {
		byte[] privateKeyBytes = Base64.getDecoder().decode(privateKey);
		PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
		PrivateKey key = keyFactory.generatePrivate(keySpec);

		Cipher cipher = Cipher.getInstance(RSA_OAEP_ALGORITHM);
		OAEPParameterSpec oaepParams = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256,
				PSource.PSpecified.DEFAULT);
		cipher.init(Cipher.DECRYPT_MODE, key, oaepParams);

		byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
		return new String(decryptedBytes);
	}

	/**
	 * Converts a Key object to Base64 string.
	 * @param key Key to convert
	 * @return Base64 encoded string of the key
	 */
	public static String keyToString(Key key) {
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

}

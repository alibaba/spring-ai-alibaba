package com.alibaba.cloud.ai.higress.api.openai;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

/**
 * @Author NGshiyu
 * @Description test
 * @CreateTime 2026/1/29 15:17
 */
public class TestAKSKHttp {

	private static String accessKeyId = System.getenv("accessKeyId");// AI网关上的消费者HMAC凭证的Access
																		// Key

	private static String accessKeySecret = System.getenv("accessKeySecret");// AI网关上的消费者HMAC凭证的Secret
																				// Key

	private static final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(100)).build();

	public static void main(String[] args) throws Exception {
		String url = "http://www.siniu.xyz/textchat/v1/chat/completions";// AI网关的接入点URL
		String jsonBody2 = "";
		String jsonBody = """
				{
				    "stream": false,
				    "max_tokens": 1024,
				    "top_p": 0.95,
				    "temperature": 1,
				    "model": "qwen-max",
				    "messages": [
				        {
				            "role": "user",
				            "content": "讲一个笑话"
				        }
				    ]
				}
				""";

		// 生成的签名串，参考：https://help.aliyun.com/zh/api-gateway/cloud-native-api-gateway/user-guide/configure-consumer-authentication
		java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
		String content_MD5 = Base64.getEncoder().encodeToString(md.digest(jsonBody.getBytes("UTF-8")));
		String nonce = UUID.randomUUID().toString();// x-ca-nonce（或标准写法
													// X-Ca-Nonce）是阿里云API网关在HMAC签名认证机制中用于防止重放攻击（Replay
													// Attack） 的关键请求头字段。
		String timestamp = String.valueOf(System.currentTimeMillis());

		// 以下签名串的内容及顺序要严格按照规定拼接，否则会导致签名验证失败
		String signString = "POST\n" + "application/json\n" + content_MD5 + "\n" + "application/json\n" + "\n"
				+ "x-ca-key:" + accessKeyId + "\n" + "x-ca-signature-method:HmacSHA256\n" + "x-ca-timestamp:"
				+ timestamp + "\n" + "/textchat/v1/chat/completions";

		System.out.println("【请求签名串】: ");
		System.out.println(signString);
		// 使用HMAC-SHA256进行签名
		String signature = generateHmac(signString, accessKeySecret, "HmacSHA256");
		System.out.println("【HMAC-SHA256签名摘要】: " + signature);

		HttpRequest request = HttpRequest.newBuilder()
			.uri(URI.create(url))
			.header("Accept", "application/json")
			.header("Content-Type", "application/json")
			.header("X-Ca-Key", accessKeyId)
			.header("X-Ca-Signature-Method", "HmacSHA256")
			.header("X-Ca-Nonce", nonce)
			.header("X-Ca-Timestamp", timestamp)
			.header("X-Ca-Signature-Headers", "x-ca-timestamp,x-ca-key,x-ca-signature-method")
			.header("X-Ca-Signature", signature)
			.header("Content-MD5", content_MD5)
			.POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
			.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println("【响应状态码】: " + response.statusCode());
		System.out.println("【响应内容】: " + response.body());
		if (response.statusCode() != 200) {
			System.out.println("【服务端的签名串】: " + response.headers().map().get("X-Ca-Error-Message"));
			// 异常处理
			// 网关签名校验失败时，会将服务端的签名串（StringToSign）放到HTTP
			// Response的Header中返回到客户端，Key为：X-Ca-Error-Message，用户只需要将本地计算的签名串（StringToSign）与服务端返回的签名串进行对比即可找到问题。如果服务端与客户端的StringToSign一致，请检查用于签名计算的AK/SK认证身份中的SK是否正确。因为HTTP
			// Header中无法表示换行，因此StringToSign中的换行符都被替换成#，
			// 参考：https://help.aliyun.com/zh/api-gateway/cloud-native-api-gateway/user-guide/configure-consumer-authentication
			// 中的异常处理章节
		}
	}

	/**
	 * 生成HMAC签名
	 * @param data 要签名的数据
	 * @param key 密钥
	 * @param algorithm HMAC算法（如HmacSHA256, HmacSHA1, HmacSHA512等）
	 * @return Base64编码的签名
	 */
	private static String generateHmac(String data, String key, String algorithm) throws Exception {
		// 创建Mac实例
		Mac mac = Mac.getInstance(algorithm);
		// 创建密钥
		SecretKeySpec secretKey = new SecretKeySpec(key.getBytes("UTF-8"), algorithm);
		// 初始化Mac
		mac.init(secretKey);
		// 计算HMAC
		byte[] hmacBytes = mac.doFinal(data.getBytes("UTF-8"));
		// Base64编码
		return Base64.getEncoder().encodeToString(hmacBytes);
	}

}

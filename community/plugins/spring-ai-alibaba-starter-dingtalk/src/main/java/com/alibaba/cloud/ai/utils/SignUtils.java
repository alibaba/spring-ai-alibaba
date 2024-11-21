package com.alibaba.cloud.ai.utils;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * @author YunLong
 */
public class SignUtils {

    public static String getSign(String signature) {
        Long timestamp = System.currentTimeMillis();
        String stringToSign = timestamp + "\n" + signature;

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signature.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(new String(Base64.encodeBase64(signData)), StandardCharsets.UTF_8);
            return "&timestamp=" + timestamp + "&sign=" + sign;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

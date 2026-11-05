/**
 * @Author: Yeharn
 * @Date:2026-11-04
 * @Version: 1.0.0
 * @Description: AES / RSA 加解密工具
 */

package org.yeharn.lp.common.util;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.StandardCharsets;

/**
 * 数据源密码加解密（AES）
 */
public final class CryptoUtils {

    private static final String PREFIX = "ENC(";
    private static final String SUFFIX = ")";
    private static String aesKey = "LearningPlatform2026AesKey!";

    private CryptoUtils() {
    }

    public static void setAesKey(String key) {
        if (StrUtil.isNotBlank(key)) {
            aesKey = key;
        }
    }

    private static AES aes() {
        byte[] key = aesKey.getBytes(StandardCharsets.UTF_8);
        if (key.length != 16 && key.length != 24 && key.length != 32) {
            byte[] fixed = new byte[16];
            System.arraycopy(key, 0, fixed, 0, Math.min(key.length, 16));
            key = fixed;
        }
        return SecureUtil.aes(key);
    }

    public static String encrypt(String plain) {
        if (StrUtil.isBlank(plain)) {
            return plain;
        }
        return PREFIX + aes().encryptBase64(plain) + SUFFIX;
    }

    public static String decrypt(String cipher) {
        if (StrUtil.isBlank(cipher) || !cipher.startsWith(PREFIX)) {
            return cipher;
        }
        String value = cipher.substring(PREFIX.length(), cipher.length() - SUFFIX.length());
        try {
            return aes().decryptStr(value);
        } catch (Exception e) {
            return cipher;
        }
    }

    public static String mask(String plain) {
        if (StrUtil.isBlank(plain)) {
            return "";
        }
        if (plain.length() <= 2) {
            return "**";
        }
        return plain.charAt(0) + "****" + plain.charAt(plain.length() - 1);
    }
}

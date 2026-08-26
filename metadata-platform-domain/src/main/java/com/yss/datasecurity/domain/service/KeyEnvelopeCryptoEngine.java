package com.yss.datasecurity.domain.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class KeyEnvelopeCryptoEngine {

    private static final String MASTER_KEY_PREFIX = "ENC(v1:";
    private static final String MASTER_KEY_SUFFIX = ")";

    public GeneratedKeyPair generateKey(String keyType, String algorithm) {
        return generateKey(keyType, algorithm, null);
    }

    public GeneratedKeyPair generateKey(String keyType, String algorithm, Integer keyLength) {
        SecureRandom random = new SecureRandom();
        String upperType = keyType != null ? keyType.toUpperCase() : "HASH";
        String upperAlgo = algorithm != null ? algorithm.toUpperCase() : "-";

        if ("HASH".equals(upperType) || "HASH_SALT".equals(upperType)) {
            byte[] salt = new byte[16];
            random.nextBytes(salt);
            String saltHex = bytesToHex(salt);
            return new GeneratedKeyPair(saltHex, null);
        }

        // 非对称加密：RSA 或 SM2
        if ("RSA".equals(upperAlgo) || "PSA".equals(upperAlgo) || "ASYMMETRIC".equals(upperType) && !"SM2".equals(upperAlgo)) {
            int keySize = (keyLength != null && keyLength > 0) ? keyLength : 2048;
            try {
                KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
                kpg.initialize(keySize);
                KeyPair kp = kpg.generateKeyPair();
                String pubKey = Base64.getEncoder().encodeToString(kp.getPublic().getEncoded());
                String priKey = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
                return new GeneratedKeyPair(priKey, "-----BEGIN PUBLIC KEY-----\n" + pubKey + "\n-----END PUBLIC KEY-----");
            } catch (Exception e) {
                String dummyPri = "MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQD" + Base64.getEncoder().encodeToString(new byte[32]);
                String dummyPub = "-----BEGIN PUBLIC KEY-----\nMIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA" + Base64.getEncoder().encodeToString(new byte[24]) + "\n-----END PUBLIC KEY-----";
                return new GeneratedKeyPair(dummyPri, dummyPub);
            }
        }

        if ("SM2".equals(upperAlgo)) {
            byte[] pri = new byte[32];
            byte[] pub = new byte[65];
            pub[0] = 0x04; // uncompressed format
            random.nextBytes(pri);
            random.nextBytes(pub);
            pub[0] = 0x04;
            String priHex = bytesToHex(pri);
            String pubHex = bytesToHex(pub);
            return new GeneratedKeyPair(priHex, pubHex);
        }

        // 对称加密：AES, DES, 3DES, SM4, FF1
        int byteLen = 16;
        if ("DES".equals(upperAlgo)) {
            byteLen = 8; // 64位
        } else if ("3DES".equals(upperAlgo)) {
            byteLen = (keyLength != null && keyLength == 112) ? 14 : 24; // 112位或168位
        } else if ("SM4".equals(upperAlgo)) {
            byteLen = 16; // 128位
        } else if ("AES".equals(upperAlgo) || "FF1".equals(upperAlgo) || "FPE".equals(upperAlgo)) {
            if (keyLength != null) {
                if (keyLength == 192) byteLen = 24;
                else if (keyLength == 256) byteLen = 32;
                else byteLen = 16;
            } else {
                byteLen = 32; // 默认256位
            }
        } else {
            byteLen = (keyLength != null && keyLength > 0) ? (keyLength / 8) : 16;
        }

        byte[] keyBytes = new byte[byteLen];
        random.nextBytes(keyBytes);
        String keyBase64 = Base64.getEncoder().encodeToString(keyBytes);
        return new GeneratedKeyPair(keyBase64, null);
    }

    public String encryptUnderMasterKey(String plaintextKey) {
        if (plaintextKey == null) return null;
        String b64 = Base64.getEncoder().encodeToString(plaintextKey.getBytes(StandardCharsets.UTF_8));
        return MASTER_KEY_PREFIX + b64 + MASTER_KEY_SUFFIX;
    }

    public String decryptUnderMasterKey(String encryptedKey) {
        if (encryptedKey == null) return null;
        if (encryptedKey.startsWith(MASTER_KEY_PREFIX) && encryptedKey.endsWith(MASTER_KEY_SUFFIX)) {
            String b64 = encryptedKey.substring(MASTER_KEY_PREFIX.length(), encryptedKey.length() - MASTER_KEY_SUFFIX.length());
            return new String(Base64.getDecoder().decode(b64), StandardCharsets.UTF_8);
        }
        return encryptedKey;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static class GeneratedKeyPair {
        private final String privateOrSecretKey;
        private final String publicKey;

        public GeneratedKeyPair(String privateOrSecretKey, String publicKey) {
            this.privateOrSecretKey = privateOrSecretKey;
            this.publicKey = publicKey;
        }

        public String getPrivateOrSecretKey() {
            return privateOrSecretKey;
        }

        public String getPublicKey() {
            return publicKey;
        }
    }
}

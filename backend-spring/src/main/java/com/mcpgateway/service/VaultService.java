package com.mcpgateway.service;

import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class VaultService {
    private static final String ENV_KEY = "MCP_GATEWAY_MASTER_KEY";
    private static final int GCM_TAG_LEN = 16 * 8; // bits

    private final byte[] key;

    public VaultService() {
        String b64 = System.getenv(ENV_KEY);
        if (b64 == null || b64.isEmpty()) {
            // generate ephemeral key for prototype
            byte[] k = new byte[32];
            new SecureRandom().nextBytes(k);
            key = k;
        } else {
            key = Base64.getDecoder().decode(b64);
        }
    }

    public byte[] encrypt(String plaintext) throws Exception {
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LEN, iv);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, spec);
        byte[] cipherText = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        ByteBuffer bb = ByteBuffer.allocate(4 + iv.length + cipherText.length);
        bb.putInt(iv.length);
        bb.put(iv);
        bb.put(cipherText);
        return bb.array();
    }

    public String decrypt(byte[] token) throws Exception {
        ByteBuffer bb = ByteBuffer.wrap(token);
        int ivlen = bb.getInt();
        byte[] iv = new byte[ivlen];
        bb.get(iv);
        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LEN, iv);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, spec);
        byte[] plain = cipher.doFinal(cipherText);
        return new String(plain, StandardCharsets.UTF_8);
    }
}

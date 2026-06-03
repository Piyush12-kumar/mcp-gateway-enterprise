package com.mcpgateway.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VaultServiceTest {
    @Test
    public void encryptDecryptRoundtrip() throws Exception {
        VaultService v = new VaultService();
        String secret = "my-secret-key-123";
        byte[] token = v.encrypt(secret);
        assertNotNull(token);
        String out = v.decrypt(token);
        assertEquals(secret, out);
    }
}

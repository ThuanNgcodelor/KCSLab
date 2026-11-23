package com.example.app.notification.customer.service;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/** Encrypts token pairs with a separately provisioned 32-byte key. Scope is authenticated as AAD. */
@Component
public class ZaloTokenCipher {
    private final CustomerNotificationProperties properties;
    public ZaloTokenCipher(CustomerNotificationProperties properties) { this.properties = properties; }

    public boolean configured() {
        try { return Base64.getDecoder().decode(properties.getTokenEncryptionKey()).length == 32; }
        catch (IllegalArgumentException exception) { return false; }
    }

    public String encrypt(String scope, String value) {
        try {
            byte[] nonce = new byte[12]; new SecureRandom().nextBytes(nonce);
            var cipher = create(Cipher.ENCRYPT_MODE, scope, nonce);
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(ByteBuffer.allocate(nonce.length + encrypted.length).put(nonce).put(encrypted).array());
        } catch (Exception exception) { throw new IllegalStateException("TOKEN_ENCRYPTION_FAILED"); }
    }

    public String decrypt(String scope, String value) {
        try {
            if (!value.startsWith("v1:")) throw new IllegalArgumentException();
            byte[] bytes = Base64.getDecoder().decode(value.substring(3));
            if (bytes.length < 28) throw new IllegalArgumentException();
            byte[] nonce = java.util.Arrays.copyOf(bytes, 12);
            return new String(create(Cipher.DECRYPT_MODE, scope, nonce).doFinal(bytes, 12, bytes.length - 12), StandardCharsets.UTF_8);
        } catch (Exception exception) { throw new IllegalStateException("TOKEN_DECRYPTION_FAILED"); }
    }

    private Cipher create(int mode, String scope, byte[] nonce) throws Exception {
        if (!configured()) throw new IllegalStateException();
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, new SecretKeySpec(Base64.getDecoder().decode(properties.getTokenEncryptionKey()), "AES"), new GCMParameterSpec(128, nonce));
        cipher.updateAAD(scope.getBytes(StandardCharsets.UTF_8));
        return cipher;
    }
}

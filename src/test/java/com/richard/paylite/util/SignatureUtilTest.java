package com.richard.paylite.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class SignatureUtilTest {

    private SignatureUtil signatureUtil;

    @BeforeEach
    void setUp() {
        signatureUtil = new SignatureUtil();
        // Use ReflectionTestUtils to set the private field 'secret'
        ReflectionTestUtils.setField(signatureUtil, "secret", "test-secret");
    }

    @Test
    void testCalculateHmacSha256() {
        String payload = "test-payload";
        String expectedSignature = "5b12467d7c448555779e70d76204105c67d27d1c991f3080c19732f9ac1988ef";
        assertEquals(expectedSignature, signatureUtil.calculateHmacSha256(payload));
    }

    @Test
    void testIsValidSignature_whenValid() {
        String payload = "test-payload";
        String validSignature = "5b12467d7c448555779e70d76204105c67d27d1c991f3080c19732f9ac1988ef";
        assertTrue(signatureUtil.isValidSignature(validSignature, payload));
    }

    @Test
    void testIsValidSignature_whenInvalid() {
        String payload = "test-payload";
        String invalidSignature = "invalid-signature";
        assertFalse(signatureUtil.isValidSignature(invalidSignature, payload));
    }
}

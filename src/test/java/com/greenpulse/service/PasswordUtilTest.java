package com.greenpulse.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordUtilTest {

    @Test
    void hashedPasswordVerifiesAndIsSalted() {
        String a = PasswordUtil.hash("admin123");
        String b = PasswordUtil.hash("admin123");
        assertNotEquals(a, b);
        assertTrue(PasswordUtil.isHashed(a));
        assertTrue(PasswordUtil.verify("admin123", a));
        assertFalse(PasswordUtil.verify("wrong", a));
    }
}

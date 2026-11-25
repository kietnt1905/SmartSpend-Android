package com.vhu.smartspend;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.vhu.smartspend.ui.RegisterActivity;

import org.junit.Test;

public class PasswordValidationTest {

    @Test
    public void testPasswordTooShort() {
        assertFalse(RegisterActivity.hasMinLength("Abc1@"));
        assertFalse(RegisterActivity.isValidPassword("Abc1@"));
    }

    @Test
    public void testPasswordMissingUppercase() {
        assertFalse(RegisterActivity.hasUpperCase("password123@"));
        assertFalse(RegisterActivity.isValidPassword("password123@"));
    }

    @Test
    public void testPasswordMissingDigit() {
        assertFalse(RegisterActivity.hasDigit("Password@"));
        assertFalse(RegisterActivity.isValidPassword("Password@"));
    }

    @Test
    public void testPasswordMissingSpecialChar() {
        assertFalse(RegisterActivity.hasSpecialChar("Password123"));
        assertFalse(RegisterActivity.isValidPassword("Password123"));
    }

    @Test
    public void testValidPassword() {
        assertTrue(RegisterActivity.isValidPassword("Password123@"));
        assertTrue(RegisterActivity.isValidPassword("Smart#Spend2026"));
        assertTrue(RegisterActivity.isValidPassword("P@ssw0rd!"));
    }
}

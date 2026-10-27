package com.greenpulse.service;

import com.greenpulse.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountRulesTest {

    @Test
    void acceptsCompleteStaffAndDriverDetails() {
        assertNull(AccountRules.problemWith("Maria Santos", "maria.santos", "recycle42", Role.STAFF));
        assertNull(AccountRules.problemWith("  Leo Cruz ", " leo_cruz ", "truckday!", Role.DRIVER));
    }

    @Test
    void nobodyCanMakeThemselvesAnAdministrator() {
        assertNotNull(AccountRules.problemWith("Maria Santos", "maria", "recycle42", Role.ADMIN));
        assertFalse(AccountRules.selfServiceRoles().contains(Role.ADMIN));
    }

    @Test
    void rejectsMissingOrMalformedDetails() {
        assertNotNull(AccountRules.problemWith(" ", "maria", "recycle42", Role.STAFF));
        assertNotNull(AccountRules.problemWith("Maria", "", "recycle42", Role.STAFF));
        assertNotNull(AccountRules.problemWith("Maria", "ma", "recycle42", Role.STAFF));
        assertNotNull(AccountRules.problemWith("Maria", "maria santos", "recycle42", Role.STAFF));
        assertNotNull(AccountRules.problemWith("Maria", "maria", "short", Role.STAFF));
        assertNotNull(AccountRules.problemWith("Maria", "maria", "recycle42", null));
    }

    @Test
    void rejectsPasswordsTheSignInFormWouldChange() {
        // The sign-in form trims what is typed, so a password with outer spaces could never be used to sign in.
        assertNotNull(AccountRules.problemWith("Maria", "maria", " recycle42", Role.STAFF));
        assertNotNull(AccountRules.problemWith("Maria", "maria", "recycle42 ", Role.STAFF));
    }
}

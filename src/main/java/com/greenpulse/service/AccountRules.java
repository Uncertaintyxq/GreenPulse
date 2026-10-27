package com.greenpulse.service;

import com.greenpulse.model.Role;
import java.util.List;
import java.util.regex.Pattern;

/** What a new account must look like. Kept apart from the form so the rules are tested without a window or a database. */
public final class AccountRules {
    public static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9._-]{3,50}");

    private AccountRules() {}

    /** Roles anyone can give themselves at sign-up. Admin is left out, so nobody can make themselves an administrator. */
    public static List<Role> selfServiceRoles() { return List.of(Role.STAFF, Role.DRIVER); }

    /** The first problem with these details, worded for the form, or {@code null} when the account can be created. */
    public static String problemWith(String fullName, String username, String password, Role role) {
        if (fullName == null || fullName.isBlank()) return "Enter your full name.";
        if (fullName.trim().length() > 100) return "Keep your full name to 100 characters or fewer.";
        if (username == null || username.isBlank()) return "Choose a username.";
        if (!USERNAME.matcher(username.trim()).matches())
            return "Usernames are 3 to 50 letters or numbers, and may use . _ or -";
        if (password == null || password.length() < MIN_PASSWORD_LENGTH)
            return "Use a password of at least " + MIN_PASSWORD_LENGTH + " characters.";
        // The sign-in form trims the password, so outer spaces would make the account impossible to sign in to.
        if (!password.equals(password.trim())) return "Passwords can't start or end with a space.";
        if (role == null || !selfServiceRoles().contains(role)) return "Choose Staff or Driver.";
        return null;
    }
}

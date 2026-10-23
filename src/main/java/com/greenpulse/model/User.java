package com.greenpulse.model;

public class User {
    private final int userId;
    private final String username;
    private final String fullName;
    private final Role role;

    public User(int userId, String username, String fullName, String role) {
        this(userId, username, fullName, Role.fromString(role));
    }

    public User(int userId, String username, String fullName, Role role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
    }

    public int getUserId() { return userId; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    /** Role name as text, kept so existing views that compare strings still work. */
    public String getRole() { return role.name(); }
    public Role getRoleType() { return role; }
}

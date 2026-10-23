package com.greenpulse.event;

/** Where alerts go. Add an email, SMS or push implementation without touching the alert logic. */
public interface NotificationChannel {
    void send(String subject, String message);
}

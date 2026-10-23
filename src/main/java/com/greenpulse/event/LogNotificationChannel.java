package com.greenpulse.event;

import java.util.logging.Logger;

/** Default channel: writes alerts to the application log. Nothing leaves the machine. */
public class LogNotificationChannel implements NotificationChannel {
    private static final Logger LOG = Logger.getLogger(LogNotificationChannel.class.getName());

    @Override
    public void send(String subject, String message) {
        LOG.warning(subject + " - " + message);
    }
}

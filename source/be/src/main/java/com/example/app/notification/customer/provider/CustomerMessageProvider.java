package com.example.app.notification.customer.provider;

public interface CustomerMessageProvider {
    record Message(String phone, String templateId, String templateData, String trackingId) {
        @Override public String toString() { return "CustomerMessage[trackingId=" + trackingId + "]"; }
    }
    record Outcome(String status, String code, String messageId) {
        public static Outcome accepted(String id) { return new Outcome("ACCEPTED", "0", id); }
        public static Outcome unknown(String code) { return new Outcome("UNKNOWN", code, null); }
    }
    Outcome send(Message message);
}

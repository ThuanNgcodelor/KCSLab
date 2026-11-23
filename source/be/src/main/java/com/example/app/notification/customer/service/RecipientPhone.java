package com.example.app.notification.customer.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class RecipientPhone {
    private RecipientPhone() {}

    public static String normalize(String raw) {
        String value = raw == null ? "" : raw.trim().replaceAll("[\\s().-]", "");
        if (value.startsWith("+")) value = value.substring(1);
        if (value.startsWith("0")) value = "84" + value.substring(1);
        if (!value.matches("84[1-9]\\d{8,9}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Số điện thoại nhận thông báo không hợp lệ");
        }
        return value;
    }

    public static String mask(String phone) {
        return "••••••" + phone.substring(Math.max(0, phone.length() - 4));
    }
}

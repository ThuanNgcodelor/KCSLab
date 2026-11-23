package com.example.app.notification.customer.provider;

import org.springframework.stereotype.Component;

@Component
public class FakeCustomerMessageProvider implements CustomerMessageProvider {
    @Override
    public Outcome send(Message message) {
        return new Outcome("SIMULATED", "FAKE", "fake-" + message.trackingId());
    }
}

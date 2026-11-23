package com.example.app.notification.customer.service;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Server-owned parameter mapping; neither template IDs nor arbitrary message text come from the client. */
@Component
public class IntakeTemplateMapper {
    private static final Set<String> SOURCES = Set.of("requestNumber", "receiptCode", "customerName", "receivedAt", "receivedDate", "sampleCount");
    private final ObjectMapper json;
    private final CustomerNotificationProperties properties;

    public IntakeTemplateMapper(ObjectMapper json, CustomerNotificationProperties properties) {
        this.json = json;
        this.properties = properties;
    }

    public Map<String, Binding> bindings() {
        try {
            Object decoded = json.readValue(properties.getBindingsJson(), Map.class);
            if (!(decoded instanceof Map<?, ?> values) || values.isEmpty() || values.size() > 30) throw new IllegalArgumentException();
            Map<String, Binding> bindings = new LinkedHashMap<>();
            for (var entry : values.entrySet()) {
                if (!(entry.getKey() instanceof String key) || !key.matches("[A-Za-z][A-Za-z0-9_]{0,79}")
                        || !(entry.getValue() instanceof Map<?, ?> spec)
                        || !(spec.get("source") instanceof String source) || !SOURCES.contains(source)) throw new IllegalArgumentException();
                Object rawLimit = spec.get("maxLength");
                int limit = rawLimit instanceof Number number ? number.intValue() : 200;
                if (limit < 1 || limit > 1000) throw new IllegalArgumentException();
                bindings.put(key, new Binding(source, limit));
            }
            if (bindings.values().stream().noneMatch(binding -> binding.source().equals("requestNumber"))) throw new IllegalArgumentException();
            return Map.copyOf(bindings);
        } catch (RuntimeException exception) {
            // Do not include raw configuration or customer data in the exception.
            throw new IllegalArgumentException("TEMPLATE_BINDINGS_INVALID");
        }
    }

    public String render(Map<String, String> source, boolean fake) {
        Map<String, String> result = new LinkedHashMap<>();
        if (fake) {
            result.putAll(source);
        } else {
            bindings().forEach((key, binding) -> {
                String value = source.get(binding.source());
                if (value == null || value.isBlank() || value.length() > binding.maxLength()) {
                    throw new IllegalArgumentException("TEMPLATE_DATA_INVALID");
                }
                result.put(key, value);
            });
        }
        return json.writeValueAsString(result);
    }

    public record Binding(String source, int maxLength) {}
}

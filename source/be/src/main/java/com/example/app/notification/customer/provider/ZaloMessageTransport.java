package com.example.app.notification.customer.provider;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import com.example.app.notification.customer.provider.CustomerMessageProvider.*;
import java.util.Map;

/** Shared wire format. Entry-point providers enforce separate development/live policies. */
final class ZaloMessageTransport {
    private ZaloMessageTransport() {}
    static Outcome send(RestClient client, ObjectMapper json, Message message, String token, boolean development) {
        try {
            var body = new java.util.LinkedHashMap<String, Object>();
            body.put("phone", message.phone()); body.put("template_id", message.templateId());
            body.put("template_data", json.readValue(message.templateData(), Map.class)); body.put("tracking_id", message.trackingId());
            if (development) body.put("mode", "development");
            return client.post().uri("/message/template").contentType(MediaType.APPLICATION_JSON)
                    .header("access_token", token).body(body)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) return Outcome.unknown("HTTP_" + response.getStatusCode().value());
                        var decoded = json.readTree(response.getBody());
                        if (!decoded.has("error") || !decoded.get("error").isIntegralNumber()) return Outcome.unknown("INVALID_RESPONSE");
                        int code = decoded.get("error").asInt();
                        if (code != 0) {
                            // -100 is explicitly unknown; other provider rejections require correction/manual retry.
                            return code == -100 ? Outcome.unknown("-100")
                                    : !development && java.util.Set.of(-115, -144, -147).contains(code)
                                        ? new Outcome("RETRY_WAIT", Integer.toString(code), null)
                                        : new Outcome("FAILED_FINAL", Integer.toString(code), null);
                        }
                        var id = decoded.path("data").path("msg_id");
                        return id.isTextual() && !id.asText().isBlank() && id.asText().length() <= 160
                                ? Outcome.accepted(id.asText()) : Outcome.unknown("MISSING_MESSAGE_ID");
                    });
        } catch (RuntimeException exception) {
            // Connection errors/timeouts can occur after Zalo accepted the request. Never log the request/token.
            return Outcome.unknown("TRANSPORT_OR_RESPONSE_ERROR");
        }
    }
}

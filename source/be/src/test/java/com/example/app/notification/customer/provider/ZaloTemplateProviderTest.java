package com.example.app.notification.customer.provider;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.service.IntakeTemplateMapper;
import com.example.app.notification.customer.service.RecipientPhone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ZaloTemplateProviderTest {
    private CustomerNotificationProperties properties;
    private MockRestServiceServer server;
    private ZaloTemplateProvider provider;
    private final CustomerMessageProvider.Message message = new CustomerMessageProvider.Message(
            "84900000099", "test-template", "{\"order_code\":\"CFC26.0001\"}", "CFCDEV1234");

    @BeforeEach
    void setUp() {
        properties = new CustomerNotificationProperties();
        properties.setMode(CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT);
        properties.setAccessToken("test-only-not-a-real-token");
        properties.setAllowedPhones("+84900000099");
        var builder = RestClient.builder().baseUrl("https://business.openapi.zalo.me");
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new ZaloTemplateProvider(properties, JsonMapper.builder().build(), builder.build());
    }

    @Test
    void sendsOnlyDevelopmentWithExactSnapshotAndReadsProviderId() {
        server.expect(requestTo("https://business.openapi.zalo.me/message/template"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("access_token", "test-only-not-a-real-token"))
                .andExpect(content().json("""
                        {"mode":"development","phone":"84900000099","template_id":"test-template",
                         "template_data":{"order_code":"CFC26.0001"},"tracking_id":"CFCDEV1234"}
                        """))
                .andRespond(withSuccess("{\"error\":0,\"data\":{\"msg_id\":\"test-message\"}}", MediaType.APPLICATION_JSON));
        assertThat(provider.send(message)).isEqualTo(CustomerMessageProvider.Outcome.accepted("test-message"));
        server.verify();
    }

    @Test
    void offEmptyAllowlistAndDifferentPhoneNeverTouchTheNetwork() {
        properties.setMode(CustomerNotificationProperties.Mode.OFF);
        assertThat(provider.send(message).status()).isEqualTo("BLOCKED");
        properties.setMode(CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT);
        properties.setAllowedPhones("");
        assertThat(provider.send(message).status()).isEqualTo("BLOCKED");
        properties.setAllowedPhones("0900000100");
        assertThat(provider.send(message).status()).isEqualTo("BLOCKED");
        properties.setAllowedPhones("invalid,0900000099");
        assertThat(provider.send(message).status()).isEqualTo("BLOCKED");
        properties.setAllowedPhones("0900000099,invalid");
        assertThat(provider.send(message).status()).isEqualTo("BLOCKED");
        server.verify();
    }

    @Test
    void http200WithBusinessErrorIsNotSuccessAndDoesNotEchoProviderText() {
        server.expect(anything()).andRespond(withSuccess("{\"error\":-131,\"message\":\"private text\"}", MediaType.APPLICATION_JSON));
        assertThat(provider.send(message)).isEqualTo(new CustomerMessageProvider.Outcome("FAILED_FINAL", "-131", null));
    }

    @Test
    void acceptedWithoutMessageIdIsUnknown() {
        server.expect(anything()).andRespond(withSuccess("{\"error\":0,\"data\":{}}", MediaType.APPLICATION_JSON));
        assertThat(provider.send(message).status()).isEqualTo("UNKNOWN");
    }

    @Test
    void timeoutAfterPossibleSendIsUnknownWithoutRetry() {
        server.expect(anything()).andRespond(withException(new IOException("test timeout")));
        assertThat(provider.send(message).status()).isEqualTo("UNKNOWN");
        server.verify();
    }

    @Test
    void serverFailureDoesNotTriggerAnotherPost() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.BAD_GATEWAY));
        assertThat(provider.send(message).status()).isEqualTo("UNKNOWN");
        server.verify();
    }

    @Test
    void invalidResponseDoesNotBecomeAccepted() {
        server.expect(anything()).andRespond(withSuccess("not json", MediaType.APPLICATION_JSON));
        assertThat(provider.send(message).status()).isEqualTo("UNKNOWN");
    }

    @Test
    void phoneNormalizationAndMappingRequireOrderIdentityAndRejectOversizedData() {
        assertThat(RecipientPhone.normalize("0900 000 099")).isEqualTo("84900000099");
        assertThat(RecipientPhone.normalize("+84900000099")).isEqualTo("84900000099");
        assertThatThrownBy(() -> RecipientPhone.normalize("x0900000099")).isInstanceOf(RuntimeException.class);
        properties.setBindingsJson("{\"order_code\":{\"source\":\"requestNumber\",\"maxLength\":12}}");
        var mapper = new IntakeTemplateMapper(JsonMapper.builder().build(), properties);
        assertThat(mapper.render(Map.of("requestNumber", "CFC26.0001"), false)).contains("order_code", "CFC26.0001");
        assertThatThrownBy(() -> mapper.render(Map.of("requestNumber", "far-too-long-request-number"), false)).isInstanceOf(IllegalArgumentException.class);
        properties.setBindingsJson("{\"name\":{\"source\":\"customerName\"}}");
        assertThatThrownBy(mapper::bindings).isInstanceOf(IllegalArgumentException.class);
    }
}

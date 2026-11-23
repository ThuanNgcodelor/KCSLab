package com.example.app.notification.customer.provider;

import com.example.app.notification.customer.config.CustomerNotificationProperties;
import com.example.app.notification.customer.service.CustomerNotificationOperations;
import com.example.app.notification.customer.service.ZaloTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class ZaloLiveTemplateProviderTest {
    CustomerNotificationProperties properties;
    ZaloTokenService tokens;
    CustomerNotificationOperations operations;
    MockRestServiceServer http;
    ZaloLiveTemplateProvider provider;
    CustomerMessageProvider.Message message = new CustomerMessageProvider.Message("84900000099", "pilot-template", "{\"order_code\":\"CFC26.0001\"}", "CFCLIVEtest");

    @BeforeEach void setup() {
        properties = new CustomerNotificationProperties(); properties.setMode(CustomerNotificationProperties.Mode.ZALO_LIVE);
        properties.setLiveEnabled(true); properties.setTokenRefreshEnabled(true); properties.setPollingEnabled(true);
        properties.setAppSecret("test-secret"); properties.setSessionKey("pilot-1"); properties.setAllowedPhones("0900000099");
        tokens = mock(ZaloTokenService.class); operations = mock(CustomerNotificationOperations.class);
        when(tokens.currentAccessToken()).thenReturn("managed-test-token");
        when(tokens.health()).thenReturn(new ZaloTokenService.Health("READY", null, null, true));
        var builder = RestClient.builder().baseUrl("https://business.openapi.zalo.me"); http = MockRestServiceServer.bindTo(builder).build();
        provider = new ZaloLiveTemplateProvider(properties, tokens, operations, JsonMapper.builder().build(), builder.build());
    }

    @Test void liveUsesSeparateGateAndOmitsDevelopmentFlag() {
        http.expect(anything()).andExpect(header("access_token", "managed-test-token"))
                .andExpect(content().json("{\"phone\":\"84900000099\",\"template_id\":\"pilot-template\",\"template_data\":{\"order_code\":\"CFC26.0001\"},\"tracking_id\":\"CFCLIVEtest\"}", org.springframework.test.json.JsonCompareMode.STRICT))
                .andRespond(withSuccess("{\"error\":0,\"data\":{\"msg_id\":\"pilot-message\"}}", MediaType.APPLICATION_JSON));
        assertThat(provider.send(message).status()).isEqualTo("ACCEPTED"); http.verify();
    }

    @Test void disabledModePauseEmptyAllowlistAndMissingManagedTokenNeverSend() {
        properties.setLiveEnabled(false); assertThat(provider.send(message).status()).isEqualTo("BLOCKED");
        properties.setLiveEnabled(true); properties.setMode(CustomerNotificationProperties.Mode.ZALO_DEVELOPMENT);
        assertThat(provider.send(message).status()).isEqualTo("BLOCKED"); properties.setMode(CustomerNotificationProperties.Mode.ZALO_LIVE);
        when(operations.paused()).thenReturn(true); assertThat(provider.send(message).status()).isEqualTo("BLOCKED"); when(operations.paused()).thenReturn(false);
        properties.setAllowedPhones(""); assertThat(provider.send(message).status()).isEqualTo("BLOCKED"); properties.setAllowedPhones("0900000099");
        when(tokens.currentAccessToken()).thenReturn(""); assertThat(provider.send(message).status()).isEqualTo("RETRY_WAIT"); http.verify();
    }

    @Test void onlyExplicitQuotaRejectionsAreRetryable() {
        for (int code : new int[]{-115, -144, -147, -124, -139, -141, -100}) {
            http.reset(); http.expect(anything()).andRespond(withSuccess("{\"error\":" + code + "}", MediaType.APPLICATION_JSON));
            assertThat(provider.send(message).status()).isEqualTo(code == -100 ? "UNKNOWN" : code == -115 || code == -144 || code == -147 ? "RETRY_WAIT" : "FAILED_FINAL");
            http.verify();
        }
    }

    @Test void timeoutAndHttpFailureDoNotAutomaticallyResend() {
        http.expect(anything()).andRespond(withException(new java.io.IOException("test timeout")));
        assertThat(provider.send(message).status()).isEqualTo("UNKNOWN"); http.verify();
    }
}

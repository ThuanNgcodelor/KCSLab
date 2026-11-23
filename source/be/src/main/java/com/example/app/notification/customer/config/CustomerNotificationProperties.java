package com.example.app.notification.customer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** No generated toString: this configuration contains an access token. */
@ConfigurationProperties("app.customer-notifications")
public class CustomerNotificationProperties {
    public enum Mode { OFF, FAKE, ZALO_DEVELOPMENT, ZALO_LIVE }
    private Mode mode = Mode.OFF;
    private String allowedPhones = "";
    private String appId = "";
    private String oaId = "";
    private String templateId = "";
    private String templateVersion = "1";
    private String bindingsJson = "{}";
    private String accessToken = "";
    private String sessionKey = "";
    private int maxCallsPerSession = 5;
    private boolean workerEnabled = true;

    private boolean webhookEnabled = false;

    private String webhookSecret = "";

    private int webhookMaxAgeSeconds = 86400;

    private boolean pollingEnabled = false;

    private int maxStatusChecks = 6;

    private boolean tokenRefreshEnabled = false;

    private String tokenEncryptionKey = "";

    private String appSecret = "";

    private boolean liveEnabled = false;

    private boolean liveAllowAllRecipients = false;

    public Mode getMode() { return mode; }
    public void setMode(Mode value) { mode = value; }
    public String getAllowedPhones() { return allowedPhones; }
    public void setAllowedPhones(String value) { allowedPhones = value; }
    public String getAppId() { return appId; }
    public void setAppId(String value) { appId = value; }
    public String getOaId() { return oaId; }
    public void setOaId(String value) { oaId = value; }
    public String getTemplateId() { return templateId; }
    public void setTemplateId(String value) { templateId = value; }
    public String getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(String value) { templateVersion = value; }
    public String getBindingsJson() { return bindingsJson; }
    public void setBindingsJson(String value) { bindingsJson = value; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String value) { accessToken = value; }
    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String value) { sessionKey = value; }
    public int getMaxCallsPerSession() { return maxCallsPerSession; }
    public void setMaxCallsPerSession(int value) { maxCallsPerSession = value; }
    public boolean isWorkerEnabled() { return workerEnabled; }
    public void setWorkerEnabled(boolean value) { workerEnabled = value; }
    public boolean isWebhookEnabled() { return webhookEnabled; }
    public void setWebhookEnabled(boolean value) { webhookEnabled = value; }
    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String value) { webhookSecret = value; }
    public int getWebhookMaxAgeSeconds() { return webhookMaxAgeSeconds; }
    public void setWebhookMaxAgeSeconds(int value) { webhookMaxAgeSeconds = value; }
    public boolean isPollingEnabled() { return pollingEnabled; }
    public void setPollingEnabled(boolean value) { pollingEnabled = value; }
    public int getMaxStatusChecks() { return maxStatusChecks; }
    public void setMaxStatusChecks(int value) { maxStatusChecks = value; }
    public boolean isTokenRefreshEnabled() { return tokenRefreshEnabled; }
    public void setTokenRefreshEnabled(boolean value) { tokenRefreshEnabled = value; }
    public String getTokenEncryptionKey() { return tokenEncryptionKey; }
    public void setTokenEncryptionKey(String value) { tokenEncryptionKey = value; }
    public String getAppSecret() { return appSecret; }
    public void setAppSecret(String value) { appSecret = value; }
    public boolean isLiveEnabled() { return liveEnabled; }
    public void setLiveEnabled(boolean value) { liveEnabled = value; }
    public boolean isLiveAllowAllRecipients() { return liveAllowAllRecipients; }
    public void setLiveAllowAllRecipients(boolean value) { liveAllowAllRecipients = value; }
}

package com.dpl.whatsapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "n8n")
public class N8nConfig {
    
    private String baseUrl;
    private Webhooks webhooks = new Webhooks();
    
    @Data
    public static class Webhooks {
        private String incomingMessage;
        private String quotationResponse;
        private String leadCreated;
        private String complaintRegistered;
        private String doRequest;
    }
    
    public String getFullWebhookUrl(String webhookPath) {
        return baseUrl + webhookPath;
    }
}

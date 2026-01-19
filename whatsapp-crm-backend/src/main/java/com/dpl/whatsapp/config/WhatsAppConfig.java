package com.dpl.whatsapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "whatsapp.api")
public class WhatsAppConfig {
    
    private String baseUrl;
    private String phoneNumberId;
    private String accessToken;
    private String verifyToken;
    private String webhookSecret;
    
    /**
     * Get the messages API endpoint
     */
    public String getMessagesUrl() {
        return baseUrl + "/" + phoneNumberId + "/messages";
    }
}

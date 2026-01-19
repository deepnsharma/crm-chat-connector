package com.dpl.whatsapp.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dynamics365")
public class Dynamics365Config {
    
    private String baseUrl;
    private String apiVersion;
    private Azure azure = new Azure();
    
    @Data
    public static class Azure {
        private String tenantId;
        private String clientId;
        private String clientSecret;
        private String scope;
    }
    
    /**
     * Get the full API URL for Dataverse Web API
     */
    public String getApiUrl() {
        return baseUrl + "/api/data/" + apiVersion;
    }
}

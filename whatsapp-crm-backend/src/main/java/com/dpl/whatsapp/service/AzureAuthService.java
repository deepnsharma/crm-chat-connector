package com.dpl.whatsapp.service;

import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.dpl.whatsapp.config.Dynamics365Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service for handling Azure AD OAuth2 authentication for Dynamics 365 Dataverse API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AzureAuthService {

    private final Dynamics365Config config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private final AtomicReference<Instant> tokenExpiry = new AtomicReference<>(Instant.MIN);

    /**
     * Get a valid access token for Dynamics 365 API calls.
     * Uses caching to avoid unnecessary token requests.
     */
    public String getAccessToken() {
        // Check if we have a valid cached token
        if (cachedToken.get() != null && Instant.now().isBefore(tokenExpiry.get().minusSeconds(60))) {
            return cachedToken.get();
        }
        
        // Request new token
        return refreshAccessToken();
    }

    /**
     * Request a new access token from Azure AD using Client Credentials flow
     */
    private synchronized String refreshAccessToken() {
        log.info("Requesting new access token from Azure AD");
        
        String tokenUrl = String.format(
            "https://login.microsoftonline.com/%s/oauth2/v2.0/token",
            config.getAzure().getTenantId()
        );
        
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", config.getAzure().getClientId());
        formData.add("client_secret", config.getAzure().getClientSecret());
        formData.add("scope", config.getAzure().getScope());
        
        try {
            String response = webClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(BodyInserters.fromFormData(formData))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            JsonNode jsonResponse = objectMapper.readTree(response);
            String accessToken = jsonResponse.get("access_token").asText();
            int expiresIn = jsonResponse.get("expires_in").asInt();
            
            // Cache the token
            cachedToken.set(accessToken);
            tokenExpiry.set(Instant.now().plusSeconds(expiresIn));
            
            log.info("Successfully obtained access token, expires in {} seconds", expiresIn);
            return accessToken;
            
        } catch (Exception e) {
            log.error("Failed to obtain access token from Azure AD", e);
            throw new RuntimeException("Failed to authenticate with Azure AD", e);
        }
    }

    /**
     * Validate the current token (for health checks)
     */
    public boolean isTokenValid() {
        try {
            getAccessToken();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

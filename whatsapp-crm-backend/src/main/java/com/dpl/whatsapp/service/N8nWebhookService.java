package com.dpl.whatsapp.service;

import com.dpl.whatsapp.config.N8nConfig;
import com.dpl.whatsapp.entity.ChatSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending notifications to n8n workflows via webhooks
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class N8nWebhookService {

    private final N8nConfig config;
    private final WebClient webClient;

    /**
     * Notify n8n when a new lead is created
     */
    @Async
    public void notifyLeadCreated(String leadId, ChatSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "LEAD_CREATED");
        payload.put("leadId", leadId);
        payload.put("phoneNumber", session.getPhoneNumber());
        payload.put("flowData", session.getFlowData());
        payload.put("timestamp", LocalDateTime.now().toString());
        
        sendToN8n(config.getWebhooks().getLeadCreated(), payload);
    }

    /**
     * Notify n8n when an opportunity is created
     */
    @Async
    public void notifyOpportunityCreated(String opportunityId, ChatSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "OPPORTUNITY_CREATED");
        payload.put("opportunityId", opportunityId);
        payload.put("customerId", session.getCustomerId());
        payload.put("customerName", session.getCustomerName());
        payload.put("phoneNumber", session.getPhoneNumber());
        payload.put("flowData", session.getFlowData());
        payload.put("timestamp", LocalDateTime.now().toString());
        
        sendToN8n(config.getWebhooks().getLeadCreated(), payload);
    }

    /**
     * Notify n8n when a complaint is registered
     */
    @Async
    public void notifyComplaintRegistered(String complaintId, ChatSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "COMPLAINT_REGISTERED");
        payload.put("complaintId", complaintId);
        payload.put("customerId", session.getCustomerId());
        payload.put("customerName", session.getCustomerName());
        payload.put("phoneNumber", session.getPhoneNumber());
        payload.put("flowData", session.getFlowData());
        payload.put("timestamp", LocalDateTime.now().toString());
        
        sendToN8n(config.getWebhooks().getComplaintRegistered(), payload);
    }

    /**
     * Notify n8n when a delivery order is created
     */
    @Async
    public void notifyDeliveryOrderCreated(String doId, ChatSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "DO_CREATED");
        payload.put("deliveryOrderId", doId);
        payload.put("customerId", session.getCustomerId());
        payload.put("customerName", session.getCustomerName());
        payload.put("phoneNumber", session.getPhoneNumber());
        payload.put("flowData", session.getFlowData());
        payload.put("timestamp", LocalDateTime.now().toString());
        
        sendToN8n(config.getWebhooks().getDoRequest(), payload);
    }

    /**
     * Notify n8n when a quote is accepted/rejected
     */
    @Async
    public void notifyQuoteResponse(String quoteId, boolean accepted, String reason, ChatSession session) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "QUOTE_RESPONSE");
        payload.put("quoteId", quoteId);
        payload.put("accepted", accepted);
        payload.put("rejectionReason", reason);
        payload.put("customerId", session.getCustomerId());
        payload.put("customerName", session.getCustomerName());
        payload.put("phoneNumber", session.getPhoneNumber());
        payload.put("timestamp", LocalDateTime.now().toString());
        
        sendToN8n(config.getWebhooks().getQuotationResponse(), payload);
    }

    /**
     * Forward incoming WhatsApp message to n8n for processing
     */
    @Async
    public void forwardIncomingMessage(Map<String, Object> messageData) {
        sendToN8n(config.getWebhooks().getIncomingMessage(), messageData);
    }

    /**
     * Send payload to n8n webhook
     */
    private void sendToN8n(String webhookPath, Map<String, Object> payload) {
        String webhookUrl = config.getFullWebhookUrl(webhookPath);
        
        try {
            webClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .subscribe(
                            response -> log.info("Successfully sent to n8n: {}", webhookPath),
                            error -> log.error("Failed to send to n8n {}: {}", webhookPath, error.getMessage())
                    );
        } catch (Exception e) {
            log.error("Error sending to n8n webhook: {}", e.getMessage());
        }
    }
}

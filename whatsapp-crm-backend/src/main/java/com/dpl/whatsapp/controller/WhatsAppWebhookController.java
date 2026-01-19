package com.dpl.whatsapp.controller;

import com.dpl.whatsapp.config.WhatsAppConfig;
import com.dpl.whatsapp.dto.whatsapp.IncomingMessageDto;
import com.dpl.whatsapp.service.ChatbotService;
import com.dpl.whatsapp.service.N8nWebhookService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for handling WhatsApp webhook callbacks from Meta
 */
@RestController
@RequestMapping("/webhook/whatsapp")
@RequiredArgsConstructor
@Slf4j
public class WhatsAppWebhookController {

    private final WhatsAppConfig config;
    private final ChatbotService chatbotService;
    private final N8nWebhookService n8nService;
    private final ObjectMapper objectMapper;

    /**
     * Webhook verification endpoint (GET request from Meta)
     * This is called by Meta to verify the webhook URL
     */
    @GetMapping
    public ResponseEntity<String> verifyWebhook(
            @RequestParam("hub.mode") String mode,
            @RequestParam("hub.verify_token") String token,
            @RequestParam("hub.challenge") String challenge) {
        
        log.info("Received webhook verification request");
        
        if ("subscribe".equals(mode) && config.getVerifyToken().equals(token)) {
            log.info("Webhook verified successfully");
            return ResponseEntity.ok(challenge);
        } else {
            log.warn("Webhook verification failed - invalid token");
            return ResponseEntity.status(403).body("Verification failed");
        }
    }

    /**
     * Webhook callback endpoint (POST request from Meta)
     * This receives all incoming messages and status updates
     */
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody String payload) {
        log.debug("Received webhook payload: {}", payload);
        
        try {
            JsonNode root = objectMapper.readTree(payload);
            
            // Check if this is a WhatsApp Business Account notification
            if (!root.has("entry")) {
                return ResponseEntity.ok("OK");
            }
            
            JsonNode entries = root.get("entry");
            for (JsonNode entry : entries) {
                if (!entry.has("changes")) continue;
                
                for (JsonNode change : entry.get("changes")) {
                    if (!"messages".equals(change.get("field").asText())) continue;
                    
                    JsonNode value = change.get("value");
                    
                    // Process messages
                    if (value.has("messages")) {
                        for (JsonNode message : value.get("messages")) {
                            processMessage(message, value);
                        }
                    }
                    
                    // Process status updates (delivered, read, etc.)
                    if (value.has("statuses")) {
                        for (JsonNode status : value.get("statuses")) {
                            processStatus(status);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error processing webhook payload", e);
        }
        
        return ResponseEntity.ok("OK");
    }

    /**
     * Process incoming message
     */
    private void processMessage(JsonNode message, JsonNode value) {
        try {
            String messageId = message.get("id").asText();
            String from = message.get("from").asText();
            String timestamp = message.get("timestamp").asText();
            String type = message.get("type").asText();
            
            IncomingMessageDto incomingMessage = new IncomingMessageDto();
            incomingMessage.setMessageId(messageId);
            incomingMessage.setFrom(from);
            incomingMessage.setTimestamp(timestamp);
            incomingMessage.setType(type);
            
            // Extract contact info if available
            if (value.has("contacts") && value.get("contacts").size() > 0) {
                JsonNode contact = value.get("contacts").get(0);
                if (contact.has("profile")) {
                    incomingMessage.setProfileName(contact.get("profile").get("name").asText());
                }
            }
            
            // Extract message content based on type
            switch (type) {
                case "text":
                    incomingMessage.setText(message.get("text").get("body").asText());
                    break;
                    
                case "interactive":
                    JsonNode interactive = message.get("interactive");
                    String interactiveType = interactive.get("type").asText();
                    
                    if ("button_reply".equals(interactiveType)) {
                        JsonNode buttonReply = interactive.get("button_reply");
                        incomingMessage.setButtonReplyId(buttonReply.get("id").asText());
                        incomingMessage.setButtonReplyTitle(buttonReply.get("title").asText());
                        
                        // Check for quote accept/reject buttons
                        String buttonId = incomingMessage.getButtonReplyId();
                        if (buttonId.startsWith("quote_accept_")) {
                            String quoteId = buttonId.replace("quote_accept_", "");
                            chatbotService.handleQuoteResponse(from, quoteId, true);
                            return;
                        } else if (buttonId.startsWith("quote_reject_")) {
                            String quoteId = buttonId.replace("quote_reject_", "");
                            chatbotService.handleQuoteResponse(from, quoteId, false);
                            return;
                        }
                        
                    } else if ("list_reply".equals(interactiveType)) {
                        JsonNode listReply = interactive.get("list_reply");
                        incomingMessage.setListReplyId(listReply.get("id").asText());
                        incomingMessage.setListReplyTitle(listReply.get("title").asText());
                    }
                    break;
                    
                case "image":
                case "document":
                case "audio":
                case "video":
                    // Handle media messages if needed
                    incomingMessage.setMediaId(message.get(type).get("id").asText());
                    if (message.get(type).has("caption")) {
                        incomingMessage.setText(message.get(type).get("caption").asText());
                    }
                    break;
                    
                case "location":
                    JsonNode location = message.get("location");
                    incomingMessage.setLatitude(location.get("latitude").asDouble());
                    incomingMessage.setLongitude(location.get("longitude").asDouble());
                    break;
            }
            
            log.info("Processing message from {}: type={}, content={}", 
                    from, type, incomingMessage.getText());
            
            // Forward to n8n for additional processing
            Map<String, Object> messageData = new HashMap<>();
            messageData.put("messageId", messageId);
            messageData.put("from", from);
            messageData.put("type", type);
            messageData.put("text", incomingMessage.getText());
            messageData.put("buttonReplyId", incomingMessage.getButtonReplyId());
            messageData.put("listReplyId", incomingMessage.getListReplyId());
            messageData.put("profileName", incomingMessage.getProfileName());
            messageData.put("timestamp", timestamp);
            n8nService.forwardIncomingMessage(messageData);
            
            // Process with chatbot
            chatbotService.processIncomingMessage(incomingMessage);
            
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }

    /**
     * Process message status update
     */
    private void processStatus(JsonNode status) {
        String messageId = status.get("id").asText();
        String statusValue = status.get("status").asText();
        String recipientId = status.get("recipient_id").asText();
        
        log.debug("Message {} to {} status: {}", messageId, recipientId, statusValue);
        
        // You can track delivery status here if needed
        // sent -> delivered -> read
    }
}

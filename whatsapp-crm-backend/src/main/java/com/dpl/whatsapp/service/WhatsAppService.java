package com.dpl.whatsapp.service;

import com.dpl.whatsapp.config.WhatsAppConfig;
import com.dpl.whatsapp.dto.whatsapp.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

/**
 * Service for sending messages via WhatsApp Business API (Meta Cloud API)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WhatsAppService {

    private final WhatsAppConfig config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    /**
     * Send a simple text message
     */
    public MessageResponse sendTextMessage(String to, String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", normalizePhoneNumber(to));
        payload.put("type", "text");
        
        Map<String, Object> text = new HashMap<>();
        text.put("preview_url", false);
        text.put("body", message);
        payload.put("text", text);

        return sendMessage(payload);
    }

    /**
     * Send an interactive message with buttons (max 3 buttons)
     */
    public MessageResponse sendButtonMessage(String to, String headerText, String bodyText, 
                                             String footerText, List<ButtonDto> buttons) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", normalizePhoneNumber(to));
        payload.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "button");

        // Header (optional)
        if (headerText != null && !headerText.isEmpty()) {
            Map<String, Object> header = new HashMap<>();
            header.put("type", "text");
            header.put("text", headerText);
            interactive.put("header", header);
        }

        // Body (required)
        Map<String, Object> body = new HashMap<>();
        body.put("text", bodyText);
        interactive.put("body", body);

        // Footer (optional)
        if (footerText != null && !footerText.isEmpty()) {
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", footerText);
            interactive.put("footer", footer);
        }

        // Buttons (max 3)
        Map<String, Object> action = new HashMap<>();
        List<Map<String, Object>> buttonList = new ArrayList<>();
        
        for (int i = 0; i < Math.min(buttons.size(), 3); i++) {
            ButtonDto btn = buttons.get(i);
            Map<String, Object> button = new HashMap<>();
            button.put("type", "reply");
            
            Map<String, Object> reply = new HashMap<>();
            reply.put("id", btn.getId());
            reply.put("title", btn.getTitle().substring(0, Math.min(btn.getTitle().length(), 20)));
            button.put("reply", reply);
            
            buttonList.add(button);
        }
        action.put("buttons", buttonList);
        interactive.put("action", action);

        payload.put("interactive", interactive);
        return sendMessage(payload);
    }

    /**
     * Send an interactive list message (for menu selection)
     */
    public MessageResponse sendListMessage(String to, String headerText, String bodyText,
                                           String footerText, String buttonText, 
                                           List<ListSectionDto> sections) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", normalizePhoneNumber(to));
        payload.put("type", "interactive");

        Map<String, Object> interactive = new HashMap<>();
        interactive.put("type", "list");

        // Header
        if (headerText != null) {
            Map<String, Object> header = new HashMap<>();
            header.put("type", "text");
            header.put("text", headerText);
            interactive.put("header", header);
        }

        // Body
        Map<String, Object> body = new HashMap<>();
        body.put("text", bodyText);
        interactive.put("body", body);

        // Footer
        if (footerText != null) {
            Map<String, Object> footer = new HashMap<>();
            footer.put("text", footerText);
            interactive.put("footer", footer);
        }

        // Action with sections
        Map<String, Object> action = new HashMap<>();
        action.put("button", buttonText);
        
        List<Map<String, Object>> sectionList = new ArrayList<>();
        for (ListSectionDto section : sections) {
            Map<String, Object> sectionMap = new HashMap<>();
            sectionMap.put("title", section.getTitle());
            
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ListRowDto row : section.getRows()) {
                Map<String, Object> rowMap = new HashMap<>();
                rowMap.put("id", row.getId());
                rowMap.put("title", row.getTitle().substring(0, Math.min(row.getTitle().length(), 24)));
                if (row.getDescription() != null) {
                    rowMap.put("description", row.getDescription().substring(0, Math.min(row.getDescription().length(), 72)));
                }
                rows.add(rowMap);
            }
            sectionMap.put("rows", rows);
            sectionList.add(sectionMap);
        }
        action.put("sections", sectionList);
        interactive.put("action", action);

        payload.put("interactive", interactive);
        return sendMessage(payload);
    }

    /**
     * Send a document (PDF, etc.)
     */
    public MessageResponse sendDocument(String to, String documentUrl, String filename, String caption) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("recipient_type", "individual");
        payload.put("to", normalizePhoneNumber(to));
        payload.put("type", "document");

        Map<String, Object> document = new HashMap<>();
        document.put("link", documentUrl);
        document.put("filename", filename);
        if (caption != null) {
            document.put("caption", caption);
        }
        payload.put("document", document);

        return sendMessage(payload);
    }

    /**
     * Send a template message (pre-approved by Meta)
     */
    public MessageResponse sendTemplateMessage(String to, String templateName, 
                                               String languageCode, List<TemplateComponentDto> components) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("to", normalizePhoneNumber(to));
        payload.put("type", "template");

        Map<String, Object> template = new HashMap<>();
        template.put("name", templateName);
        
        Map<String, Object> language = new HashMap<>();
        language.put("code", languageCode);
        template.put("language", language);

        if (components != null && !components.isEmpty()) {
            List<Map<String, Object>> componentList = new ArrayList<>();
            for (TemplateComponentDto comp : components) {
                Map<String, Object> component = new HashMap<>();
                component.put("type", comp.getType());
                
                if (comp.getParameters() != null) {
                    List<Map<String, Object>> params = new ArrayList<>();
                    for (TemplateParameterDto param : comp.getParameters()) {
                        Map<String, Object> paramMap = new HashMap<>();
                        paramMap.put("type", param.getType());
                        if ("text".equals(param.getType())) {
                            paramMap.put("text", param.getValue());
                        } else if ("document".equals(param.getType())) {
                            Map<String, Object> doc = new HashMap<>();
                            doc.put("link", param.getValue());
                            doc.put("filename", param.getFilename());
                            paramMap.put("document", doc);
                        }
                        params.add(paramMap);
                    }
                    component.put("parameters", params);
                }
                componentList.add(component);
            }
            template.put("components", componentList);
        }

        payload.put("template", template);
        return sendMessage(payload);
    }

    /**
     * Mark a message as read
     */
    public void markAsRead(String messageId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("messaging_product", "whatsapp");
        payload.put("status", "read");
        payload.put("message_id", messageId);

        try {
            webClient.post()
                    .uri(config.getMessagesUrl())
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            log.debug("Marked message {} as read", messageId);
        } catch (Exception e) {
            log.warn("Failed to mark message as read: {}", e.getMessage());
        }
    }

    /**
     * Common method to send messages
     */
    private MessageResponse sendMessage(Map<String, Object> payload) {
        try {
            String response = webClient.post()
                    .uri(config.getMessagesUrl())
                    .header("Authorization", "Bearer " + config.getAccessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode jsonResponse = objectMapper.readTree(response);
            
            MessageResponse result = new MessageResponse();
            result.setSuccess(true);
            
            if (jsonResponse.has("messages") && jsonResponse.get("messages").size() > 0) {
                result.setMessageId(jsonResponse.get("messages").get(0).get("id").asText());
            }
            
            log.info("Successfully sent WhatsApp message: {}", result.getMessageId());
            return result;

        } catch (WebClientResponseException e) {
            log.error("WhatsApp API error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            
            MessageResponse error = new MessageResponse();
            error.setSuccess(false);
            error.setError(e.getResponseBodyAsString());
            return error;
            
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message", e);
            
            MessageResponse error = new MessageResponse();
            error.setSuccess(false);
            error.setError(e.getMessage());
            return error;
        }
    }

    /**
     * Normalize phone number to international format
     */
    private String normalizePhoneNumber(String phone) {
        // Remove all non-digit characters
        String normalized = phone.replaceAll("[^0-9]", "");
        
        // Ensure it starts with country code (assuming India if not present)
        if (!normalized.startsWith("91") && normalized.length() == 10) {
            normalized = "91" + normalized;
        }
        
        return normalized;
    }
}

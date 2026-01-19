package com.dpl.whatsapp.service;

import com.dpl.whatsapp.dto.crm.*;
import com.dpl.whatsapp.dto.whatsapp.*;
import com.dpl.whatsapp.entity.ChatSession;
import com.dpl.whatsapp.entity.ConversationState;
import com.dpl.whatsapp.repository.ChatSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Core chatbot logic for handling WhatsApp conversations
 * Implements state machine pattern for conversation flow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatbotService {

    private final WhatsAppService whatsAppService;
    private final DataverseService dataverseService;
    private final ChatSessionRepository sessionRepository;
    private final N8nWebhookService n8nService;

    // Conversation states
    public enum State {
        INITIAL,
        MAIN_MENU,
        // Lead Flow
        LEAD_NAME,
        LEAD_COMPANY,
        LEAD_EMAIL,
        LEAD_PRODUCT_INTEREST,
        LEAD_QUANTITY,
        LEAD_CONFIRM,
        // Complaint Flow
        COMPLAINT_TYPE,
        COMPLAINT_DESCRIPTION,
        COMPLAINT_PRIORITY,
        COMPLAINT_CONFIRM,
        // DO Flow
        DO_SELECT_ORDER,
        DO_QUANTITY,
        DO_DELIVERY_DATE,
        DO_ADDRESS,
        DO_CONFIRM,
        // Quote Response
        QUOTE_REASON,
        // General
        AWAITING_RESPONSE
    }

    /**
     * Process incoming WhatsApp message
     */
    public void processIncomingMessage(IncomingMessageDto message) {
        String phoneNumber = message.getFrom();
        String messageText = message.getText();
        String buttonId = message.getButtonReplyId();
        String listId = message.getListReplyId();

        log.info("Processing message from {}: {}", phoneNumber, messageText);

        // Mark message as read
        whatsAppService.markAsRead(message.getMessageId());

        // Get or create session
        ChatSession session = getOrCreateSession(phoneNumber);

        // Check if user is an existing customer
        Optional<CustomerDto> customer = dataverseService.getCustomerByPhone(phoneNumber);
        if (customer.isPresent()) {
            session.setCustomerId(customer.get().getAccountId());
            session.setContactId(customer.get().getContactId());
            session.setCustomerName(customer.get().getAccountName());
        }

        // Process based on current state
        processState(session, messageText, buttonId, listId);
    }

    /**
     * Process message based on current conversation state
     */
    private void processState(ChatSession session, String text, String buttonId, String listId) {
        String input = buttonId != null ? buttonId : (listId != null ? listId : text);
        State currentState = State.valueOf(session.getCurrentState());

        switch (currentState) {
            case INITIAL:
                handleInitialState(session);
                break;

            case MAIN_MENU:
                handleMainMenu(session, input);
                break;

            // Lead Flow States
            case LEAD_NAME:
                handleLeadName(session, text);
                break;
            case LEAD_COMPANY:
                handleLeadCompany(session, text);
                break;
            case LEAD_EMAIL:
                handleLeadEmail(session, text);
                break;
            case LEAD_PRODUCT_INTEREST:
                handleLeadProductInterest(session, text);
                break;
            case LEAD_QUANTITY:
                handleLeadQuantity(session, text);
                break;
            case LEAD_CONFIRM:
                handleLeadConfirm(session, input);
                break;

            // Complaint Flow States
            case COMPLAINT_TYPE:
                handleComplaintType(session, input);
                break;
            case COMPLAINT_DESCRIPTION:
                handleComplaintDescription(session, text);
                break;
            case COMPLAINT_PRIORITY:
                handleComplaintPriority(session, input);
                break;
            case COMPLAINT_CONFIRM:
                handleComplaintConfirm(session, input);
                break;

            // DO Flow States
            case DO_SELECT_ORDER:
                handleDoSelectOrder(session, input);
                break;
            case DO_QUANTITY:
                handleDoQuantity(session, text);
                break;
            case DO_DELIVERY_DATE:
                handleDoDeliveryDate(session, text);
                break;
            case DO_ADDRESS:
                handleDoAddress(session, text);
                break;
            case DO_CONFIRM:
                handleDoConfirm(session, input);
                break;

            // Quote Response
            case QUOTE_REASON:
                handleQuoteReason(session, text);
                break;

            default:
                handleInitialState(session);
        }
    }

    // ==================== INITIAL & MENU HANDLERS ====================

    private void handleInitialState(ChatSession session) {
        String greeting;
        if (session.getCustomerName() != null) {
            greeting = String.format("Hello %s! 👋\nWelcome to DPL Customer Service.", session.getCustomerName());
        } else {
            greeting = "Hello! 👋\nWelcome to DPL Customer Service.";
        }

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("menu_inquiry", "New Inquiry"),
                new ButtonDto("menu_complaint", "Register Complaint"),
                new ButtonDto("menu_do", "Book Delivery")
        );

        whatsAppService.sendButtonMessage(
                session.getPhoneNumber(),
                "DPL WhatsApp Service",
                greeting + "\n\nHow can I help you today?",
                "Reply with your choice",
                buttons
        );

        updateState(session, State.MAIN_MENU);
    }

    private void handleMainMenu(ChatSession session, String choice) {
        switch (choice.toLowerCase()) {
            case "menu_inquiry":
            case "1":
            case "inquiry":
            case "new inquiry":
                startLeadFlow(session);
                break;
            case "menu_complaint":
            case "2":
            case "complaint":
                startComplaintFlow(session);
                break;
            case "menu_do":
            case "3":
            case "delivery":
            case "book delivery":
                startDeliveryOrderFlow(session);
                break;
            default:
                whatsAppService.sendTextMessage(session.getPhoneNumber(),
                        "I didn't understand that. Please select an option from the menu.");
                handleInitialState(session);
        }
    }

    // ==================== LEAD FLOW HANDLERS ====================

    private void startLeadFlow(ChatSession session) {
        session.setFlowData(new HashMap<>());
        
        if (session.getCustomerId() != null) {
            // Existing customer - create opportunity
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "Great! I'll help you submit a new inquiry.\n\nPlease describe the product you're interested in:");
            updateState(session, State.LEAD_PRODUCT_INTEREST);
        } else {
            // New customer - create lead
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "Great! I'll help you submit an inquiry.\n\nPlease provide your full name:");
            updateState(session, State.LEAD_NAME);
        }
    }

    private void handleLeadName(ChatSession session, String name) {
        session.getFlowData().put("name", name);
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "Thank you, " + name + "!\n\nPlease provide your company name:");
        updateState(session, State.LEAD_COMPANY);
    }

    private void handleLeadCompany(ChatSession session, String company) {
        session.getFlowData().put("company", company);
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "Got it!\n\nPlease provide your email address:");
        updateState(session, State.LEAD_EMAIL);
    }

    private void handleLeadEmail(ChatSession session, String email) {
        // Basic email validation
        if (!email.contains("@") || !email.contains(".")) {
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "That doesn't look like a valid email. Please provide a valid email address:");
            return;
        }
        session.getFlowData().put("email", email);
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "What product are you interested in?");
        updateState(session, State.LEAD_PRODUCT_INTEREST);
    }

    private void handleLeadProductInterest(ChatSession session, String product) {
        session.getFlowData().put("product", product);
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "What quantity are you looking for? (in MT)");
        updateState(session, State.LEAD_QUANTITY);
    }

    private void handleLeadQuantity(ChatSession session, String quantity) {
        session.getFlowData().put("quantity", quantity);

        // Build confirmation message
        Map<String, String> data = session.getFlowData();
        String summary;
        
        if (session.getCustomerId() != null) {
            summary = String.format(
                    "Please confirm your inquiry:\n\n" +
                    "📦 Product: %s\n" +
                    "📊 Quantity: %s MT\n\n" +
                    "Is this correct?",
                    data.get("product"), quantity
            );
        } else {
            summary = String.format(
                    "Please confirm your details:\n\n" +
                    "👤 Name: %s\n" +
                    "🏢 Company: %s\n" +
                    "📧 Email: %s\n" +
                    "📦 Product: %s\n" +
                    "📊 Quantity: %s MT\n\n" +
                    "Is this correct?",
                    data.get("name"), data.get("company"), data.get("email"),
                    data.get("product"), quantity
            );
        }

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("confirm_yes", "✅ Yes, Submit"),
                new ButtonDto("confirm_no", "❌ No, Cancel")
        );

        whatsAppService.sendButtonMessage(session.getPhoneNumber(),
                "Confirm Inquiry", summary, null, buttons);
        updateState(session, State.LEAD_CONFIRM);
    }

    private void handleLeadConfirm(ChatSession session, String response) {
        if ("confirm_yes".equals(response) || response.toLowerCase().contains("yes")) {
            Map<String, String> data = session.getFlowData();
            
            try {
                if (session.getCustomerId() != null) {
                    // Create opportunity for existing customer
                    OpportunityDto opportunity = new OpportunityDto();
                    opportunity.setName("WhatsApp Inquiry - " + data.get("product"));
                    opportunity.setDescription("Product: " + data.get("product") + "\nQuantity: " + data.get("quantity") + " MT");
                    opportunity.setAccountId(session.getCustomerId());
                    opportunity.setContactId(session.getContactId());
                    
                    String oppId = dataverseService.createOpportunity(opportunity);
                    
                    whatsAppService.sendTextMessage(session.getPhoneNumber(),
                            "✅ Your inquiry has been submitted successfully!\n\n" +
                            "Reference: OPP-" + oppId.substring(0, 8).toUpperCase() + "\n\n" +
                            "Our sales team will contact you shortly. Thank you!");
                    
                    // Notify n8n
                    n8nService.notifyOpportunityCreated(oppId, session);
                    
                } else {
                    // Create lead for new customer
                    LeadDto lead = new LeadDto();
                    String[] nameParts = data.get("name").split(" ", 2);
                    lead.setFirstName(nameParts[0]);
                    lead.setLastName(nameParts.length > 1 ? nameParts[1] : "");
                    lead.setCompanyName(data.get("company"));
                    lead.setEmail(data.get("email"));
                    lead.setPhone(session.getPhoneNumber());
                    lead.setSubject("WhatsApp Lead - " + data.get("product"));
                    lead.setDescription("Product Interest: " + data.get("product") + "\nQuantity: " + data.get("quantity") + " MT");
                    
                    String leadId = dataverseService.createLead(lead);
                    
                    whatsAppService.sendTextMessage(session.getPhoneNumber(),
                            "✅ Thank you for your inquiry!\n\n" +
                            "Reference: LEAD-" + leadId.substring(0, 8).toUpperCase() + "\n\n" +
                            "Our sales team will contact you shortly.");
                    
                    // Notify n8n
                    n8nService.notifyLeadCreated(leadId, session);
                }
            } catch (Exception e) {
                log.error("Failed to create lead/opportunity", e);
                whatsAppService.sendTextMessage(session.getPhoneNumber(),
                        "Sorry, there was an error processing your request. Please try again later or contact us directly.");
            }
        } else {
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "No problem! Your inquiry has been cancelled.");
        }

        resetSession(session);
    }

    // ==================== COMPLAINT FLOW HANDLERS ====================

    private void startComplaintFlow(ChatSession session) {
        if (session.getCustomerId() == null) {
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "I couldn't find your account in our system. Please contact our support team directly or provide your customer ID.");
            resetSession(session);
            return;
        }

        session.setFlowData(new HashMap<>());

        List<ListSectionDto> sections = new ArrayList<>();
        List<ListRowDto> rows = Arrays.asList(
                new ListRowDto("complaint_quality", "Quality Issue", "Product quality related complaints"),
                new ListRowDto("complaint_delivery", "Delivery Issue", "Late or wrong delivery"),
                new ListRowDto("complaint_billing", "Billing Issue", "Invoice or payment related"),
                new ListRowDto("complaint_other", "Other", "Other issues")
        );
        sections.add(new ListSectionDto("Complaint Types", rows));

        whatsAppService.sendListMessage(session.getPhoneNumber(),
                "Register Complaint",
                "Please select the type of issue you're facing:",
                null,
                "Select Type",
                sections);

        updateState(session, State.COMPLAINT_TYPE);
    }

    private void handleComplaintType(ChatSession session, String type) {
        session.getFlowData().put("type", type);
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "Please describe the issue in detail:");
        updateState(session, State.COMPLAINT_DESCRIPTION);
    }

    private void handleComplaintDescription(ChatSession session, String description) {
        session.getFlowData().put("description", description);

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("priority_high", "🔴 High"),
                new ButtonDto("priority_normal", "🟡 Normal"),
                new ButtonDto("priority_low", "🟢 Low")
        );

        whatsAppService.sendButtonMessage(session.getPhoneNumber(),
                "Priority",
                "How urgent is this issue?",
                null,
                buttons);
        updateState(session, State.COMPLAINT_PRIORITY);
    }

    private void handleComplaintPriority(ChatSession session, String priority) {
        int priorityCode = "priority_high".equals(priority) ? 1 :
                          "priority_low".equals(priority) ? 3 : 2;
        session.getFlowData().put("priority", String.valueOf(priorityCode));
        session.getFlowData().put("priorityLabel", priority.replace("priority_", "").toUpperCase());

        Map<String, String> data = session.getFlowData();
        String summary = String.format(
                "Please confirm your complaint:\n\n" +
                "📋 Type: %s\n" +
                "📝 Description: %s\n" +
                "⚡ Priority: %s\n\n" +
                "Submit this complaint?",
                data.get("type").replace("complaint_", "").toUpperCase(),
                data.get("description"),
                data.get("priorityLabel")
        );

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("confirm_yes", "✅ Yes, Submit"),
                new ButtonDto("confirm_no", "❌ No, Cancel")
        );

        whatsAppService.sendButtonMessage(session.getPhoneNumber(),
                "Confirm Complaint", summary, null, buttons);
        updateState(session, State.COMPLAINT_CONFIRM);
    }

    private void handleComplaintConfirm(ChatSession session, String response) {
        if ("confirm_yes".equals(response) || response.toLowerCase().contains("yes")) {
            Map<String, String> data = session.getFlowData();

            try {
                ComplaintDto complaint = new ComplaintDto();
                complaint.setTitle("WhatsApp Complaint - " + data.get("type").replace("complaint_", "").toUpperCase());
                complaint.setDescription(data.get("description"));
                complaint.setPriority(Integer.parseInt(data.get("priority")));
                complaint.setAccountId(session.getCustomerId());
                complaint.setContactId(session.getContactId());

                String complaintId = dataverseService.createComplaint(complaint);

                whatsAppService.sendTextMessage(session.getPhoneNumber(),
                        "✅ Your complaint has been registered!\n\n" +
                        "Ticket ID: CASE-" + complaintId.substring(0, 8).toUpperCase() + "\n\n" +
                        "Our team will investigate and get back to you shortly. Thank you for your patience.");

                // Notify n8n
                n8nService.notifyComplaintRegistered(complaintId, session);

            } catch (Exception e) {
                log.error("Failed to create complaint", e);
                whatsAppService.sendTextMessage(session.getPhoneNumber(),
                        "Sorry, there was an error registering your complaint. Please try again later.");
            }
        } else {
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "Complaint registration cancelled.");
        }

        resetSession(session);
    }

    // ==================== DELIVERY ORDER FLOW HANDLERS ====================

    private void startDeliveryOrderFlow(ChatSession session) {
        if (session.getCustomerId() == null) {
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "I couldn't find your account in our system. Please contact our sales team for assistance.");
            resetSession(session);
            return;
        }

        session.setFlowData(new HashMap<>());

        // Get active sales orders for this customer
        try {
            List<SalesOrderDto> orders = dataverseService.getSalesOrdersByCustomer(session.getCustomerId());

            if (orders.isEmpty()) {
                whatsAppService.sendTextMessage(session.getPhoneNumber(),
                        "You don't have any active orders to book a delivery against. Please contact your sales representative.");
                resetSession(session);
                return;
            }

            List<ListSectionDto> sections = new ArrayList<>();
            List<ListRowDto> rows = new ArrayList<>();
            
            for (SalesOrderDto order : orders) {
                rows.add(new ListRowDto(
                        "order_" + order.getOrderId(),
                        order.getOrderNumber(),
                        order.getName() + " - ₹" + String.format("%.2f", order.getTotalAmount())
                ));
            }
            sections.add(new ListSectionDto("Your Orders", rows));

            whatsAppService.sendListMessage(session.getPhoneNumber(),
                    "Book Delivery Order",
                    "Select the order you want to book delivery against:",
                    null,
                    "Select Order",
                    sections);

            updateState(session, State.DO_SELECT_ORDER);

        } catch (Exception e) {
            log.error("Failed to fetch orders", e);
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "Sorry, couldn't fetch your orders. Please try again later.");
            resetSession(session);
        }
    }

    private void handleDoSelectOrder(ChatSession session, String orderId) {
        String actualOrderId = orderId.replace("order_", "");
        session.getFlowData().put("orderId", actualOrderId);
        
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "What quantity do you want to book for delivery? (in MT)");
        updateState(session, State.DO_QUANTITY);
    }

    private void handleDoQuantity(ChatSession session, String quantity) {
        session.getFlowData().put("quantity", quantity);
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "When do you need the delivery? (Please provide date in DD/MM/YYYY format)");
        updateState(session, State.DO_DELIVERY_DATE);
    }

    private void handleDoDeliveryDate(ChatSession session, String date) {
        session.getFlowData().put("deliveryDate", date);
        whatsAppService.sendTextMessage(session.getPhoneNumber(),
                "Please provide the delivery address:");
        updateState(session, State.DO_ADDRESS);
    }

    private void handleDoAddress(ChatSession session, String address) {
        session.getFlowData().put("address", address);

        Map<String, String> data = session.getFlowData();
        String summary = String.format(
                "Please confirm your delivery order:\n\n" +
                "📦 Quantity: %s MT\n" +
                "📅 Delivery Date: %s\n" +
                "📍 Address: %s\n\n" +
                "Confirm this delivery order?",
                data.get("quantity"), data.get("deliveryDate"), address
        );

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("confirm_yes", "✅ Yes, Confirm"),
                new ButtonDto("confirm_no", "❌ No, Cancel")
        );

        whatsAppService.sendButtonMessage(session.getPhoneNumber(),
                "Confirm Delivery", summary, null, buttons);
        updateState(session, State.DO_CONFIRM);
    }

    private void handleDoConfirm(ChatSession session, String response) {
        if ("confirm_yes".equals(response) || response.toLowerCase().contains("yes")) {
            Map<String, String> data = session.getFlowData();

            try {
                DeliveryOrderDto deliveryOrder = new DeliveryOrderDto();
                deliveryOrder.setName("DO-" + System.currentTimeMillis());
                deliveryOrder.setSalesOrderId(data.get("orderId"));
                deliveryOrder.setQuantity(Double.parseDouble(data.get("quantity")));
                deliveryOrder.setDeliveryDate(data.get("deliveryDate"));
                deliveryOrder.setDeliveryAddress(data.get("address"));
                deliveryOrder.setAccountId(session.getCustomerId());

                String doId = dataverseService.createDeliveryOrder(deliveryOrder);

                whatsAppService.sendTextMessage(session.getPhoneNumber(),
                        "✅ Your delivery order has been created!\n\n" +
                        "DO Number: DO-" + doId.substring(0, 8).toUpperCase() + "\n\n" +
                        "You will receive confirmation once it's processed. Thank you!");

                // Notify n8n
                n8nService.notifyDeliveryOrderCreated(doId, session);

            } catch (Exception e) {
                log.error("Failed to create delivery order", e);
                whatsAppService.sendTextMessage(session.getPhoneNumber(),
                        "Sorry, there was an error creating your delivery order. Please contact your sales representative.");
            }
        } else {
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "Delivery order cancelled.");
        }

        resetSession(session);
    }

    // ==================== QUOTE RESPONSE HANDLERS ====================

    /**
     * Handle quote acceptance/rejection from push message buttons
     */
    public void handleQuoteResponse(String phoneNumber, String quoteId, boolean accepted) {
        ChatSession session = getOrCreateSession(phoneNumber);
        session.getFlowData().put("quoteId", quoteId);
        session.getFlowData().put("accepted", String.valueOf(accepted));

        if (accepted) {
            // Accept quote
            try {
                dataverseService.updateQuoteStatus(quoteId, true, null);
                whatsAppService.sendTextMessage(phoneNumber,
                        "✅ Thank you for accepting the quote!\n\n" +
                        "Our team will process this and create your sales order shortly.");
                
                n8nService.notifyQuoteResponse(quoteId, true, null, session);
            } catch (Exception e) {
                log.error("Failed to accept quote", e);
                whatsAppService.sendTextMessage(phoneNumber,
                        "Sorry, there was an error processing your acceptance. Please contact your sales representative.");
            }
            resetSession(session);
        } else {
            // Ask for rejection reason
            whatsAppService.sendTextMessage(phoneNumber,
                    "We're sorry to hear that. Could you please tell us why you're declining the quote?\n\n" +
                    "(Your feedback helps us serve you better)");
            updateState(session, State.QUOTE_REASON);
        }
    }

    private void handleQuoteReason(ChatSession session, String reason) {
        String quoteId = session.getFlowData().get("quoteId");

        try {
            dataverseService.updateQuoteStatus(quoteId, false, reason);
            whatsAppService.sendTextMessage(session.getPhoneNumber(),
                    "Thank you for your feedback. Your key account manager will contact you to discuss alternatives.\n\n" +
                    "Is there anything else we can help you with?");

            n8nService.notifyQuoteResponse(quoteId, false, reason, session);
        } catch (Exception e) {
            log.error("Failed to reject quote", e);
        }

        resetSession(session);
    }

    // ==================== SESSION MANAGEMENT ====================

    private ChatSession getOrCreateSession(String phoneNumber) {
        return sessionRepository.findByPhoneNumber(phoneNumber)
                .orElseGet(() -> {
                    ChatSession newSession = new ChatSession();
                    newSession.setPhoneNumber(phoneNumber);
                    newSession.setCurrentState(State.INITIAL.name());
                    newSession.setFlowData(new HashMap<>());
                    newSession.setCreatedAt(LocalDateTime.now());
                    newSession.setUpdatedAt(LocalDateTime.now());
                    return sessionRepository.save(newSession);
                });
    }

    private void updateState(ChatSession session, State newState) {
        session.setCurrentState(newState.name());
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private void resetSession(ChatSession session) {
        session.setCurrentState(State.INITIAL.name());
        session.setFlowData(new HashMap<>());
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }
}

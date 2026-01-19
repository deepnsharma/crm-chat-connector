package com.dpl.whatsapp.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "chat_sessions")
@Data
public class ChatSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    
    @Column(unique = true, nullable = false)
    private String phoneNumber;
    
    private String customerId;
    private String contactId;
    private String customerName;
    private String currentState;
    
    @ElementCollection
    @CollectionTable(name = "chat_session_data")
    @MapKeyColumn(name = "data_key")
    @Column(name = "data_value")
    private Map<String, String> flowData = new HashMap<>();
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

enum ConversationState { INITIAL, MAIN_MENU, LEAD_NAME, LEAD_COMPANY, LEAD_EMAIL, LEAD_PRODUCT_INTEREST, LEAD_QUANTITY, LEAD_CONFIRM, COMPLAINT_TYPE, COMPLAINT_DESCRIPTION, COMPLAINT_PRIORITY, COMPLAINT_CONFIRM, DO_SELECT_ORDER, DO_QUANTITY, DO_DELIVERY_DATE, DO_ADDRESS, DO_CONFIRM, QUOTE_REASON, AWAITING_RESPONSE }

package com.dpl.whatsapp.dto.crm;

import lombok.Data;
import java.util.Map;

@Data
public class CustomerDto {
    private String accountId;
    private String accountName;
    private String accountNumber;
    private String contactId;
    private String firstName;
    private String lastName;
    private String phone;
    private String mobile;
    private String email;
}

@Data
class LeadDto {
    private String subject;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String companyName;
    private String description;
    private Map<String, Object> customFields;
}

@Data
class QuoteDto {
    private String quoteId;
    private String quoteNumber;
    private String name;
    private double totalAmount;
    private int stateCode;
    private int statusCode;
    private String customerName;
}

@Data
class OpportunityDto {
    private String name;
    private String description;
    private Double estimatedValue;
    private String accountId;
    private String contactId;
    private Map<String, Object> customFields;
}

@Data
class ComplaintDto {
    private String complaintId;
    private String title;
    private String description;
    private int priority;
    private String accountId;
    private String contactId;
    private int stateCode;
    private int statusCode;
    private Map<String, Object> customFields;
}

@Data
class SalesOrderDto {
    private String orderId;
    private String orderNumber;
    private String name;
    private double totalAmount;
    private String requestDeliveryBy;
}

@Data
class DeliveryOrderDto {
    private String name;
    private String salesOrderId;
    private double quantity;
    private String deliveryDate;
    private String deliveryAddress;
    private String remarks;
    private String accountId;
}

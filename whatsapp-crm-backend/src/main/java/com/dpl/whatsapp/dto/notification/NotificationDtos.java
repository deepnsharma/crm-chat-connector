package com.dpl.whatsapp.dto.notification;

import lombok.Data;

@Data public class CustomerOnboardedDto { private String phoneNumber; private String customerName; private String salesPersonName; }
@Data public class QuotationNotificationDto { private String phoneNumber; private String customerName; private String quoteNumber; private double totalAmount; private String pdfUrl; private String quoteId; }
@Data public class SalesOrderNotificationDto { private String phoneNumber; private String customerName; private String orderNumber; private double totalAmount; }
@Data public class DeliveryOrderNotificationDto { private String phoneNumber; private String customerName; private String doNumber; private String deliveryDate; private double quantity; }
@Data public class InvoiceNotificationDto { private String phoneNumber; private String customerName; private String invoiceNumber; private double amount; private String pdfUrl; private String trackingLink; }
@Data public class ComplaintNotificationDto { private String phoneNumber; private String customerName; private String caseNumber; private String issueType; }
@Data public class ComplaintResolvedDto { private String phoneNumber; private String customerName; private String caseNumber; private String resolution; }
@Data public class GreetingDto { private String phoneNumber; private String customerName; }
@Data public class FestivalGreetingDto { private String phoneNumber; private String customerName; private String festivalName; private String customMessage; }
@Data public class ShipmentReminderDto { private String phoneNumber; private String customerName; private String orderNumber; private String lastDate; private double pendingQuantity; }
@Data public class ContractExpiryDto { private String phoneNumber; private String customerName; private String contractNumber; private String expiryDate; }
@Data public class CreditLimitDto { private String phoneNumber; private String customerName; private double currentOutstanding; private double creditLimit; }
@Data public class ApprovalRequestDto { private String phoneNumber; private String approverName; private String requestType; private String requestDetails; private String requestId; }

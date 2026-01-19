package com.dpl.whatsapp.controller;

import com.dpl.whatsapp.dto.notification.*;
import com.dpl.whatsapp.dto.whatsapp.MessageResponse;
import com.dpl.whatsapp.service.PushNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for sending push notifications to customers
 * These endpoints are called by CRM triggers or n8n workflows
 */
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Push Notifications", description = "Endpoints for sending WhatsApp notifications to customers")
public class PushNotificationController {

    private final PushNotificationService notificationService;

    @PostMapping("/customer-onboarded")
    @Operation(summary = "Send customer onboarding welcome message")
    public ResponseEntity<MessageResponse> sendCustomerOnboarded(@RequestBody CustomerOnboardedDto request) {
        MessageResponse response = notificationService.sendCustomerOnboardedMessage(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getSalesPersonName()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/quotation")
    @Operation(summary = "Send quotation with accept/reject buttons")
    public ResponseEntity<MessageResponse> sendQuotation(@RequestBody QuotationNotificationDto request) {
        MessageResponse response = notificationService.sendQuotation(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getQuoteNumber(),
                request.getTotalAmount(),
                request.getPdfUrl(),
                request.getQuoteId()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/sales-order-created")
    @Operation(summary = "Send sales order confirmation")
    public ResponseEntity<MessageResponse> sendSalesOrderCreated(@RequestBody SalesOrderNotificationDto request) {
        MessageResponse response = notificationService.sendSalesOrderCreated(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getOrderNumber(),
                request.getTotalAmount()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delivery-order-created")
    @Operation(summary = "Send delivery order notification")
    public ResponseEntity<MessageResponse> sendDoCreated(@RequestBody DeliveryOrderNotificationDto request) {
        MessageResponse response = notificationService.sendDoCreated(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getDoNumber(),
                request.getDeliveryDate(),
                request.getQuantity()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/invoice")
    @Operation(summary = "Send invoice with tracking link")
    public ResponseEntity<MessageResponse> sendInvoice(@RequestBody InvoiceNotificationDto request) {
        MessageResponse response = notificationService.sendInvoice(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getInvoiceNumber(),
                request.getAmount(),
                request.getPdfUrl(),
                request.getTrackingLink()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaint-registered")
    @Operation(summary = "Send complaint registration confirmation")
    public ResponseEntity<MessageResponse> sendComplaintRegistered(@RequestBody ComplaintNotificationDto request) {
        MessageResponse response = notificationService.sendComplaintRegistered(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getCaseNumber(),
                request.getIssueType()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/complaint-resolved")
    @Operation(summary = "Send complaint resolution notification")
    public ResponseEntity<MessageResponse> sendComplaintResolved(@RequestBody ComplaintResolvedDto request) {
        MessageResponse response = notificationService.sendComplaintResolved(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getCaseNumber(),
                request.getResolution()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/birthday")
    @Operation(summary = "Send birthday greeting")
    public ResponseEntity<MessageResponse> sendBirthdayGreeting(@RequestBody GreetingDto request) {
        MessageResponse response = notificationService.sendBirthdayGreeting(
                request.getPhoneNumber(),
                request.getCustomerName()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/festival")
    @Operation(summary = "Send festival greeting")
    public ResponseEntity<MessageResponse> sendFestivalGreeting(@RequestBody FestivalGreetingDto request) {
        MessageResponse response = notificationService.sendFestivalGreeting(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getFestivalName(),
                request.getCustomMessage()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/shipment-reminder")
    @Operation(summary = "Send shipment pending reminder")
    public ResponseEntity<MessageResponse> sendShipmentReminder(@RequestBody ShipmentReminderDto request) {
        MessageResponse response = notificationService.sendShipmentPendingReminder(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getOrderNumber(),
                request.getLastDate(),
                request.getPendingQuantity()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/contract-expiry")
    @Operation(summary = "Send contract expiry notification")
    public ResponseEntity<MessageResponse> sendContractExpiry(@RequestBody ContractExpiryDto request) {
        MessageResponse response = notificationService.sendContractExpiryNotification(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getContractNumber(),
                request.getExpiryDate()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/credit-limit-exceeded")
    @Operation(summary = "Send credit limit exceeded alert")
    public ResponseEntity<MessageResponse> sendCreditLimitExceeded(@RequestBody CreditLimitDto request) {
        MessageResponse response = notificationService.sendCreditLimitExceeded(
                request.getPhoneNumber(),
                request.getCustomerName(),
                request.getCurrentOutstanding(),
                request.getCreditLimit()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/approval-request")
    @Operation(summary = "Send approval request (internal)")
    public ResponseEntity<MessageResponse> sendApprovalRequest(@RequestBody ApprovalRequestDto request) {
        MessageResponse response = notificationService.sendApprovalRequest(
                request.getPhoneNumber(),
                request.getApproverName(),
                request.getRequestType(),
                request.getRequestDetails(),
                request.getRequestId()
        );
        return ResponseEntity.ok(response);
    }
}

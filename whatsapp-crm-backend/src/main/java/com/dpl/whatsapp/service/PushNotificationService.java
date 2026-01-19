package com.dpl.whatsapp.service;

import com.dpl.whatsapp.dto.whatsapp.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * Service for sending push notifications (outbound messages) to customers
 * These are triggered by CRM events
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

    private final WhatsAppService whatsAppService;

    /**
     * Send customer onboarding welcome message
     */
    public MessageResponse sendCustomerOnboardedMessage(String phoneNumber, String customerName, String salesPersonName) {
        String message = String.format(
                "🎉 Welcome to DPL, %s!\n\n" +
                "We're thrilled to have you as our valued customer.\n\n" +
                "Your Key Account Manager: %s\n\n" +
                "For any queries, feel free to reach out to us on this WhatsApp number.\n\n" +
                "Thank you for choosing DPL!",
                customerName, salesPersonName
        );
        
        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send quotation with accept/reject buttons
     */
    public MessageResponse sendQuotation(String phoneNumber, String customerName, 
                                         String quoteNumber, double totalAmount, 
                                         String pdfUrl, String quoteId) {
        // First send the document
        whatsAppService.sendDocument(
                phoneNumber,
                pdfUrl,
                "Quote_" + quoteNumber + ".pdf",
                String.format("📄 Quotation %s\nTotal Amount: ₹%.2f", quoteNumber, totalAmount)
        );

        // Then send interactive buttons for response
        String message = String.format(
                "Dear %s,\n\n" +
                "Please find attached quotation %s.\n\n" +
                "💰 Total Amount: ₹%.2f\n\n" +
                "Please review and let us know your decision.",
                customerName, quoteNumber, totalAmount
        );

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("quote_accept_" + quoteId, "✅ Accept"),
                new ButtonDto("quote_reject_" + quoteId, "❌ Decline")
        );

        return whatsAppService.sendButtonMessage(
                phoneNumber,
                "Quotation " + quoteNumber,
                message,
                "Tap to respond",
                buttons
        );
    }

    /**
     * Send sales order confirmation
     */
    public MessageResponse sendSalesOrderCreated(String phoneNumber, String customerName, 
                                                 String orderNumber, double totalAmount) {
        String message = String.format(
                "✅ Sales Order Created!\n\n" +
                "Dear %s,\n\n" +
                "Your order has been confirmed:\n\n" +
                "📋 Order Number: %s\n" +
                "💰 Total Amount: ₹%.2f\n\n" +
                "You will receive updates as your order progresses.\n\n" +
                "Thank you for your business!",
                customerName, orderNumber, totalAmount
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send Delivery Order (DO) creation notification
     */
    public MessageResponse sendDoCreated(String phoneNumber, String customerName,
                                         String doNumber, String deliveryDate, double quantity) {
        String message = String.format(
                "📦 Delivery Order Created!\n\n" +
                "Dear %s,\n\n" +
                "Your delivery has been scheduled:\n\n" +
                "🔖 DO Number: %s\n" +
                "📊 Quantity: %.2f MT\n" +
                "📅 Expected Delivery: %s\n\n" +
                "You will receive tracking updates closer to the delivery date.",
                customerName, doNumber, quantity, deliveryDate
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send invoice with tracking link
     */
    public MessageResponse sendInvoice(String phoneNumber, String customerName,
                                       String invoiceNumber, double amount,
                                       String pdfUrl, String trackingLink) {
        // Send invoice PDF
        whatsAppService.sendDocument(
                phoneNumber,
                pdfUrl,
                "Invoice_" + invoiceNumber + ".pdf",
                "📄 Invoice " + invoiceNumber
        );

        String message = String.format(
                "📧 Invoice Generated!\n\n" +
                "Dear %s,\n\n" +
                "Invoice Number: %s\n" +
                "Amount: ₹%.2f\n\n" +
                "🚚 Track your shipment: %s\n\n" +
                "Thank you!",
                customerName, invoiceNumber, amount, trackingLink
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send complaint registered confirmation
     */
    public MessageResponse sendComplaintRegistered(String phoneNumber, String customerName,
                                                   String caseNumber, String issueType) {
        String message = String.format(
                "📝 Complaint Registered\n\n" +
                "Dear %s,\n\n" +
                "Your complaint has been registered:\n\n" +
                "🎫 Ticket Number: %s\n" +
                "📋 Issue Type: %s\n\n" +
                "Our team is looking into this and will get back to you within 24-48 hours.\n\n" +
                "Thank you for your patience.",
                customerName, caseNumber, issueType
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send complaint resolved notification
     */
    public MessageResponse sendComplaintResolved(String phoneNumber, String customerName,
                                                 String caseNumber, String resolution) {
        String message = String.format(
                "✅ Complaint Resolved\n\n" +
                "Dear %s,\n\n" +
                "Your complaint (Ticket: %s) has been resolved.\n\n" +
                "Resolution: %s\n\n" +
                "If you have any further concerns, please don't hesitate to reach out.\n\n" +
                "Thank you for your understanding.",
                customerName, caseNumber, resolution
        );

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("feedback_satisfied", "👍 Satisfied"),
                new ButtonDto("feedback_not_satisfied", "👎 Not Satisfied")
        );

        return whatsAppService.sendButtonMessage(
                phoneNumber,
                "Complaint Resolved",
                message,
                "Rate our service",
                buttons
        );
    }

    /**
     * Send birthday greeting
     */
    public MessageResponse sendBirthdayGreeting(String phoneNumber, String customerName) {
        String message = String.format(
                "🎂 Happy Birthday, %s! 🎉\n\n" +
                "Wishing you a wonderful day filled with joy and happiness.\n\n" +
                "Best wishes from the DPL Team! 🌟",
                customerName
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send festival greeting
     */
    public MessageResponse sendFestivalGreeting(String phoneNumber, String customerName, 
                                                String festivalName, String customMessage) {
        String message = String.format(
                "🌟 Happy %s, %s! 🌟\n\n%s\n\nWarm wishes from DPL!",
                festivalName, customerName, customMessage
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send shipment pending reminder (3 days before last date)
     */
    public MessageResponse sendShipmentPendingReminder(String phoneNumber, String customerName,
                                                       String orderNumber, String lastDate,
                                                       double pendingQty) {
        String message = String.format(
                "⚠️ Shipment Reminder\n\n" +
                "Dear %s,\n\n" +
                "This is a reminder that your order has pending quantity:\n\n" +
                "📋 Order: %s\n" +
                "📊 Pending: %.2f MT\n" +
                "📅 Last Date: %s\n\n" +
                "Please arrange for shipment to avoid any delays.\n\n" +
                "Contact your sales representative for assistance.",
                customerName, orderNumber, pendingQty, lastDate
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send contract expiry notification
     */
    public MessageResponse sendContractExpiryNotification(String phoneNumber, String customerName,
                                                          String contractNumber, String expiryDate) {
        String message = String.format(
                "📋 Contract Expiry Notice\n\n" +
                "Dear %s,\n\n" +
                "Your contract is expiring soon:\n\n" +
                "📄 Contract: %s\n" +
                "📅 Expiry Date: %s\n\n" +
                "Please contact your Key Account Manager to discuss renewal.\n\n" +
                "We look forward to continuing our partnership!",
                customerName, contractNumber, expiryDate
        );

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("contract_renew", "📝 Request Renewal"),
                new ButtonDto("contract_discuss", "💬 Discuss Options")
        );

        return whatsAppService.sendButtonMessage(
                phoneNumber,
                "Contract Expiring",
                message,
                null,
                buttons
        );
    }

    /**
     * Send credit limit exceeded notification
     */
    public MessageResponse sendCreditLimitExceeded(String phoneNumber, String customerName,
                                                   double currentOutstanding, double creditLimit) {
        String message = String.format(
                "⚠️ Credit Limit Alert\n\n" +
                "Dear %s,\n\n" +
                "Your account has exceeded the credit limit:\n\n" +
                "💳 Credit Limit: ₹%.2f\n" +
                "📊 Outstanding: ₹%.2f\n\n" +
                "Please clear outstanding payments to continue placing orders.\n\n" +
                "Contact your Account Manager for assistance.",
                customerName, creditLimit, currentOutstanding
        );

        return whatsAppService.sendTextMessage(phoneNumber, message);
    }

    /**
     * Send approval request notification (for internal use)
     */
    public MessageResponse sendApprovalRequest(String phoneNumber, String approverName,
                                               String requestType, String requestDetails,
                                               String requestId) {
        String message = String.format(
                "📋 Approval Required\n\n" +
                "Dear %s,\n\n" +
                "A new %s requires your approval:\n\n" +
                "%s\n\n" +
                "Please review and take action.",
                approverName, requestType, requestDetails
        );

        List<ButtonDto> buttons = Arrays.asList(
                new ButtonDto("approve_" + requestId, "✅ Approve"),
                new ButtonDto("reject_" + requestId, "❌ Reject")
        );

        return whatsAppService.sendButtonMessage(
                phoneNumber,
                "Approval Request",
                message,
                "Tap to respond",
                buttons
        );
    }
}

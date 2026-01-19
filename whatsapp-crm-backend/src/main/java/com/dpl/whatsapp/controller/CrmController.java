package com.dpl.whatsapp.controller;

import com.dpl.whatsapp.dto.crm.*;
import com.dpl.whatsapp.service.DataverseService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * REST Controller for CRM/Dataverse operations
 * These endpoints can be called by n8n workflows
 */
@RestController
@RequestMapping("/crm")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "CRM Operations", description = "Endpoints for Dynamics 365 CRM operations")
public class CrmController {

    private final DataverseService dataverseService;

    // ==================== CUSTOMER ENDPOINTS ====================

    @GetMapping("/customers")
    @Operation(summary = "Get all customers with contacts")
    public ResponseEntity<List<CustomerDto>> getAllCustomers() {
        return ResponseEntity.ok(dataverseService.getAllCustomers());
    }

    @GetMapping("/customers/by-phone/{phone}")
    @Operation(summary = "Get customer by phone number")
    public ResponseEntity<CustomerDto> getCustomerByPhone(@PathVariable String phone) {
        Optional<CustomerDto> customer = dataverseService.getCustomerByPhone(phone);
        return customer.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==================== LEAD ENDPOINTS ====================

    @PostMapping("/leads")
    @Operation(summary = "Create a new lead")
    public ResponseEntity<String> createLead(@RequestBody LeadDto lead) {
        String leadId = dataverseService.createLead(lead);
        return ResponseEntity.ok(leadId);
    }

    // ==================== QUOTE ENDPOINTS ====================

    @GetMapping("/quotes/{quoteId}")
    @Operation(summary = "Get quote by ID")
    public ResponseEntity<QuoteDto> getQuote(@PathVariable String quoteId) {
        Optional<QuoteDto> quote = dataverseService.getQuoteById(quoteId);
        return quote.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/quotes/{quoteId}/status")
    @Operation(summary = "Update quote status (accept/reject)")
    public ResponseEntity<Void> updateQuoteStatus(
            @PathVariable String quoteId,
            @RequestParam boolean accepted,
            @RequestParam(required = false) String reason) {
        dataverseService.updateQuoteStatus(quoteId, accepted, reason);
        return ResponseEntity.ok().build();
    }

    // ==================== OPPORTUNITY ENDPOINTS ====================

    @PostMapping("/opportunities")
    @Operation(summary = "Create a new opportunity")
    public ResponseEntity<String> createOpportunity(@RequestBody OpportunityDto opportunity) {
        String oppId = dataverseService.createOpportunity(opportunity);
        return ResponseEntity.ok(oppId);
    }

    // ==================== COMPLAINT ENDPOINTS ====================

    @PostMapping("/complaints")
    @Operation(summary = "Create a new complaint/case")
    public ResponseEntity<String> createComplaint(@RequestBody ComplaintDto complaint) {
        String complaintId = dataverseService.createComplaint(complaint);
        return ResponseEntity.ok(complaintId);
    }

    @GetMapping("/complaints/customer/{accountId}")
    @Operation(summary = "Get complaints for a customer")
    public ResponseEntity<List<ComplaintDto>> getCustomerComplaints(@PathVariable String accountId) {
        return ResponseEntity.ok(dataverseService.getComplaintsByCustomer(accountId));
    }

    // ==================== SALES ORDER ENDPOINTS ====================

    @GetMapping("/orders/customer/{accountId}")
    @Operation(summary = "Get sales orders for a customer")
    public ResponseEntity<List<SalesOrderDto>> getCustomerOrders(@PathVariable String accountId) {
        return ResponseEntity.ok(dataverseService.getSalesOrdersByCustomer(accountId));
    }

    // ==================== DELIVERY ORDER ENDPOINTS ====================

    @PostMapping("/delivery-orders")
    @Operation(summary = "Create a delivery order")
    public ResponseEntity<String> createDeliveryOrder(@RequestBody DeliveryOrderDto deliveryOrder) {
        String doId = dataverseService.createDeliveryOrder(deliveryOrder);
        return ResponseEntity.ok(doId);
    }

    // ==================== METADATA ENDPOINTS ====================

    @GetMapping("/entities/{entityName}/metadata")
    @Operation(summary = "Get entity metadata (fields, attributes)")
    public ResponseEntity<JsonNode> getEntityMetadata(@PathVariable String entityName) {
        return ResponseEntity.ok(dataverseService.getEntityMetadata(entityName));
    }

    @GetMapping("/entities/custom")
    @Operation(summary = "Get all custom entities in the system")
    public ResponseEntity<JsonNode> getCustomEntities() {
        return ResponseEntity.ok(dataverseService.getCustomEntities());
    }

    // ==================== GENERIC QUERY ENDPOINT ====================

    @GetMapping("/query/{entitySet}")
    @Operation(summary = "Execute a custom OData query")
    public ResponseEntity<JsonNode> executeQuery(
            @PathVariable String entitySet,
            @RequestParam(required = false) String filter,
            @RequestParam(required = false) String select,
            @RequestParam(required = false) String expand,
            @RequestParam(required = false) String orderby,
            @RequestParam(required = false, defaultValue = "50") int top) {
        
        StringBuilder query = new StringBuilder("?");
        if (filter != null) query.append("$filter=").append(filter).append("&");
        if (select != null) query.append("$select=").append(select).append("&");
        if (expand != null) query.append("$expand=").append(expand).append("&");
        if (orderby != null) query.append("$orderby=").append(orderby).append("&");
        query.append("$top=").append(top);
        
        return ResponseEntity.ok(dataverseService.get(entitySet, query.toString()));
    }
}

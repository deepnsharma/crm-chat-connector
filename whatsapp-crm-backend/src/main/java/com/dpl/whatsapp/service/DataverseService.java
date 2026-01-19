package com.dpl.whatsapp.service;

import com.dpl.whatsapp.config.Dynamics365Config;
import com.dpl.whatsapp.dto.crm.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * Service for interacting with Microsoft Dynamics 365 Dataverse Web API
 * Supports both standard and custom entities
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DataverseService {

    private final Dynamics365Config config;
    private final AzureAuthService authService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // ==================== GENERIC CRUD OPERATIONS ====================

    /**
     * Execute a GET request to Dataverse API
     */
    public JsonNode get(String entitySet, String query) {
        String url = buildUrl(entitySet, query);
        log.debug("GET request to Dataverse: {}", url);
        
        try {
            String response = webClient.get()
                    .uri(url)
                    .headers(this::setHeaders)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return objectMapper.readTree(response);
        } catch (WebClientResponseException e) {
            log.error("Dataverse GET error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Dataverse API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to parse Dataverse response", e);
            throw new RuntimeException("Failed to process Dataverse response", e);
        }
    }

    /**
     * Execute a POST request to create a new record
     */
    public String create(String entitySet, Object entity) {
        String url = config.getApiUrl() + "/" + entitySet;
        log.debug("POST request to Dataverse: {}", url);
        
        try {
            String jsonBody = objectMapper.writeValueAsString(entity);
            
            return webClient.post()
                    .uri(url)
                    .headers(this::setHeaders)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .toBodilessEntity()
                    .map(response -> {
                        // Extract the created entity ID from OData-EntityId header
                        String entityId = response.getHeaders().getFirst("OData-EntityId");
                        if (entityId != null) {
                            // Extract GUID from URL like: https://org.crm.dynamics.com/api/data/v9.2/accounts(guid)
                            int start = entityId.lastIndexOf("(") + 1;
                            int end = entityId.lastIndexOf(")");
                            return entityId.substring(start, end);
                        }
                        return null;
                    })
                    .block();
                    
        } catch (WebClientResponseException e) {
            log.error("Dataverse POST error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to create record: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to create Dataverse record", e);
            throw new RuntimeException("Failed to create record", e);
        }
    }

    /**
     * Execute a PATCH request to update an existing record
     */
    public void update(String entitySet, String entityId, Object entity) {
        String url = config.getApiUrl() + "/" + entitySet + "(" + entityId + ")";
        log.debug("PATCH request to Dataverse: {}", url);
        
        try {
            String jsonBody = objectMapper.writeValueAsString(entity);
            
            webClient.patch()
                    .uri(url)
                    .headers(this::setHeaders)
                    .bodyValue(jsonBody)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
                    
            log.info("Successfully updated record: {}/{}", entitySet, entityId);
            
        } catch (WebClientResponseException e) {
            log.error("Dataverse PATCH error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to update record: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("Failed to update Dataverse record", e);
            throw new RuntimeException("Failed to update record", e);
        }
    }

    /**
     * Execute a DELETE request
     */
    public void delete(String entitySet, String entityId) {
        String url = config.getApiUrl() + "/" + entitySet + "(" + entityId + ")";
        log.debug("DELETE request to Dataverse: {}", url);
        
        try {
            webClient.delete()
                    .uri(url)
                    .headers(this::setHeaders)
                    .retrieve()
                    .toBodilessEntity()
                    .block();
                    
            log.info("Successfully deleted record: {}/{}", entitySet, entityId);
            
        } catch (WebClientResponseException e) {
            log.error("Dataverse DELETE error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Failed to delete record: " + e.getMessage(), e);
        }
    }

    // ==================== CUSTOMER/ACCOUNT OPERATIONS ====================

    /**
     * Get customer (account) by phone number
     */
    public Optional<CustomerDto> getCustomerByPhone(String phoneNumber) {
        // Normalize phone number (remove spaces, dashes)
        String normalizedPhone = phoneNumber.replaceAll("[\\s\\-()]", "");
        
        // Search in contacts associated with accounts
        String query = String.format(
            "?$filter=contains(telephone1,'%s') or contains(mobilephone,'%s')" +
            "&$expand=parentcustomerid_account($select=accountid,name,accountnumber)" +
            "&$select=contactid,firstname,lastname,telephone1,mobilephone,emailaddress1",
            normalizedPhone, normalizedPhone
        );
        
        JsonNode result = get("contacts", query);
        JsonNode contacts = result.get("value");
        
        if (contacts != null && contacts.size() > 0) {
            JsonNode contact = contacts.get(0);
            return Optional.of(mapToCustomerDto(contact));
        }
        
        return Optional.empty();
    }

    /**
     * Get all customers with their contacts
     */
    public List<CustomerDto> getAllCustomers() {
        String query = "?$select=accountid,name,accountnumber" +
                      "&$expand=contact_customer_accounts($select=contactid,firstname,lastname,telephone1,mobilephone,emailaddress1)" +
                      "&$top=100";
        
        JsonNode result = get("accounts", query);
        List<CustomerDto> customers = new ArrayList<>();
        
        JsonNode accounts = result.get("value");
        if (accounts != null) {
            for (JsonNode account : accounts) {
                customers.add(mapAccountToCustomerDto(account));
            }
        }
        
        return customers;
    }

    /**
     * Create a new lead in CRM
     */
    public String createLead(LeadDto lead) {
        Map<String, Object> leadData = new HashMap<>();
        leadData.put("subject", lead.getSubject());
        leadData.put("firstname", lead.getFirstName());
        leadData.put("lastname", lead.getLastName());
        leadData.put("telephone1", lead.getPhone());
        leadData.put("emailaddress1", lead.getEmail());
        leadData.put("companyname", lead.getCompanyName());
        leadData.put("description", lead.getDescription());
        
        // Add custom fields if present
        if (lead.getCustomFields() != null) {
            leadData.putAll(lead.getCustomFields());
        }
        
        String leadId = create("leads", leadData);
        log.info("Created new lead with ID: {}", leadId);
        return leadId;
    }

    // ==================== QUOTE OPERATIONS ====================

    /**
     * Get quote by ID
     */
    public Optional<QuoteDto> getQuoteById(String quoteId) {
        String query = String.format("(%s)?$select=quoteid,quotenumber,name,totalamount,statecode,statuscode" +
                "&$expand=customerid_account($select=name)", quoteId);
        
        try {
            JsonNode result = get("quotes", query);
            return Optional.of(mapToQuoteDto(result));
        } catch (Exception e) {
            log.error("Quote not found: {}", quoteId);
            return Optional.empty();
        }
    }

    /**
     * Update quote status (accept/reject)
     */
    public void updateQuoteStatus(String quoteId, boolean accepted, String reason) {
        Map<String, Object> updateData = new HashMap<>();
        
        if (accepted) {
            // Status: Won = 4
            updateData.put("statecode", 1); // Active
            updateData.put("statuscode", 4); // Won
        } else {
            // Status: Lost = 5
            updateData.put("statecode", 2); // Closed
            updateData.put("statuscode", 5); // Lost
            updateData.put("description", "Rejected via WhatsApp. Reason: " + reason);
        }
        
        update("quotes", quoteId, updateData);
        log.info("Updated quote {} status to {}", quoteId, accepted ? "Accepted" : "Rejected");
    }

    // ==================== OPPORTUNITY OPERATIONS ====================

    /**
     * Create an opportunity for existing customer
     */
    public String createOpportunity(OpportunityDto opportunity) {
        Map<String, Object> oppData = new HashMap<>();
        oppData.put("name", opportunity.getName());
        oppData.put("description", opportunity.getDescription());
        oppData.put("estimatedvalue", opportunity.getEstimatedValue());
        
        // Link to existing account
        if (opportunity.getAccountId() != null) {
            oppData.put("parentaccountid@odata.bind", "/accounts(" + opportunity.getAccountId() + ")");
        }
        
        // Link to contact
        if (opportunity.getContactId() != null) {
            oppData.put("parentcontactid@odata.bind", "/contacts(" + opportunity.getContactId() + ")");
        }
        
        // Add custom fields
        if (opportunity.getCustomFields() != null) {
            oppData.putAll(opportunity.getCustomFields());
        }
        
        String oppId = create("opportunities", oppData);
        log.info("Created new opportunity with ID: {}", oppId);
        return oppId;
    }

    // ==================== COMPLAINT OPERATIONS ====================

    /**
     * Create a complaint (case) in CRM
     * Note: Complaints are stored in 'incidents' entity (standard) or custom entity
     */
    public String createComplaint(ComplaintDto complaint) {
        Map<String, Object> caseData = new HashMap<>();
        caseData.put("title", complaint.getTitle());
        caseData.put("description", complaint.getDescription());
        caseData.put("caseorigincode", 3); // WhatsApp origin
        caseData.put("prioritycode", complaint.getPriority()); // 1=High, 2=Normal, 3=Low
        
        // Link to customer
        if (complaint.getAccountId() != null) {
            caseData.put("customerid_account@odata.bind", "/accounts(" + complaint.getAccountId() + ")");
        }
        if (complaint.getContactId() != null) {
            caseData.put("primarycontactid@odata.bind", "/contacts(" + complaint.getContactId() + ")");
        }
        
        // Add custom fields for complaint details
        if (complaint.getCustomFields() != null) {
            caseData.putAll(complaint.getCustomFields());
        }
        
        String caseId = create("incidents", caseData);
        log.info("Created new complaint/case with ID: {}", caseId);
        return caseId;
    }

    /**
     * Get complaints for a customer
     */
    public List<ComplaintDto> getComplaintsByCustomer(String accountId) {
        String query = String.format(
            "?$filter=_customerid_value eq '%s'&$select=incidentid,title,description,statecode,statuscode,createdon" +
            "&$orderby=createdon desc", accountId
        );
        
        JsonNode result = get("incidents", query);
        List<ComplaintDto> complaints = new ArrayList<>();
        
        JsonNode cases = result.get("value");
        if (cases != null) {
            for (JsonNode caseNode : cases) {
                complaints.add(mapToComplaintDto(caseNode));
            }
        }
        
        return complaints;
    }

    // ==================== SALES ORDER OPERATIONS ====================

    /**
     * Get sales orders for a customer
     */
    public List<SalesOrderDto> getSalesOrdersByCustomer(String accountId) {
        String query = String.format(
            "?$filter=_customerid_value eq '%s' and statecode eq 0" +
            "&$select=salesorderid,ordernumber,name,totalamount,requestdeliveryby" +
            "&$orderby=createdon desc", accountId
        );
        
        JsonNode result = get("salesorders", query);
        List<SalesOrderDto> orders = new ArrayList<>();
        
        JsonNode ordersNode = result.get("value");
        if (ordersNode != null) {
            for (JsonNode order : ordersNode) {
                orders.add(mapToSalesOrderDto(order));
            }
        }
        
        return orders;
    }

    // ==================== CUSTOM ENTITY OPERATIONS ====================
    
    /**
     * Create a Delivery Order (DO) - Custom Entity
     * Adjust the entity name based on your Dataverse schema
     */
    public String createDeliveryOrder(DeliveryOrderDto deliveryOrder) {
        Map<String, Object> doData = new HashMap<>();
        
        // Standard fields
        doData.put("cr_name", deliveryOrder.getName());
        doData.put("cr_quantity", deliveryOrder.getQuantity());
        doData.put("cr_deliverydate", deliveryOrder.getDeliveryDate());
        doData.put("cr_deliveryaddress", deliveryOrder.getDeliveryAddress());
        doData.put("cr_remarks", deliveryOrder.getRemarks());
        
        // Link to Sales Order
        if (deliveryOrder.getSalesOrderId() != null) {
            doData.put("cr_salesorderid@odata.bind", "/salesorders(" + deliveryOrder.getSalesOrderId() + ")");
        }
        
        // Link to Customer
        if (deliveryOrder.getAccountId() != null) {
            doData.put("cr_customerid@odata.bind", "/accounts(" + deliveryOrder.getAccountId() + ")");
        }
        
        // Custom entity name - adjust based on your schema
        String entityName = "cr_deliveryorders";
        String doId = create(entityName, doData);
        log.info("Created new Delivery Order with ID: {}", doId);
        return doId;
    }

    /**
     * Get entity metadata to discover custom entities and their fields
     */
    public JsonNode getEntityMetadata(String entityLogicalName) {
        String url = config.getApiUrl() + "/EntityDefinitions(LogicalName='" + entityLogicalName + "')" +
                    "?$select=LogicalName,DisplayName,PrimaryIdAttribute,PrimaryNameAttribute" +
                    "&$expand=Attributes($select=LogicalName,DisplayName,AttributeType,RequiredLevel)";
        
        try {
            String response = webClient.get()
                    .uri(url)
                    .headers(this::setHeaders)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to get entity metadata for: {}", entityLogicalName, e);
            throw new RuntimeException("Failed to get entity metadata", e);
        }
    }

    /**
     * Get all custom entities in the system
     */
    public JsonNode getCustomEntities() {
        String url = config.getApiUrl() + "/EntityDefinitions" +
                    "?$filter=IsCustomEntity eq true" +
                    "&$select=LogicalName,DisplayName,Description";
        
        try {
            String response = webClient.get()
                    .uri(url)
                    .headers(this::setHeaders)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to get custom entities", e);
            throw new RuntimeException("Failed to get custom entities", e);
        }
    }

    // ==================== HELPER METHODS ====================

    private void setHeaders(HttpHeaders headers) {
        headers.setBearerAuth(authService.getAccessToken());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("OData-MaxVersion", "4.0");
        headers.set("OData-Version", "4.0");
        headers.set("Prefer", "odata.include-annotations=*");
    }

    private String buildUrl(String entitySet, String query) {
        String url = config.getApiUrl() + "/" + entitySet;
        if (query != null && !query.isEmpty()) {
            url += query;
        }
        return url;
    }

    private CustomerDto mapToCustomerDto(JsonNode contact) {
        CustomerDto customer = new CustomerDto();
        customer.setContactId(getTextValue(contact, "contactid"));
        customer.setFirstName(getTextValue(contact, "firstname"));
        customer.setLastName(getTextValue(contact, "lastname"));
        customer.setPhone(getTextValue(contact, "telephone1"));
        customer.setMobile(getTextValue(contact, "mobilephone"));
        customer.setEmail(getTextValue(contact, "emailaddress1"));
        
        JsonNode account = contact.get("parentcustomerid_account");
        if (account != null && !account.isNull()) {
            customer.setAccountId(getTextValue(account, "accountid"));
            customer.setAccountName(getTextValue(account, "name"));
            customer.setAccountNumber(getTextValue(account, "accountnumber"));
        }
        
        return customer;
    }

    private CustomerDto mapAccountToCustomerDto(JsonNode account) {
        CustomerDto customer = new CustomerDto();
        customer.setAccountId(getTextValue(account, "accountid"));
        customer.setAccountName(getTextValue(account, "name"));
        customer.setAccountNumber(getTextValue(account, "accountnumber"));
        
        JsonNode contacts = account.get("contact_customer_accounts");
        if (contacts != null && contacts.size() > 0) {
            JsonNode primaryContact = contacts.get(0);
            customer.setContactId(getTextValue(primaryContact, "contactid"));
            customer.setFirstName(getTextValue(primaryContact, "firstname"));
            customer.setLastName(getTextValue(primaryContact, "lastname"));
            customer.setPhone(getTextValue(primaryContact, "telephone1"));
            customer.setMobile(getTextValue(primaryContact, "mobilephone"));
            customer.setEmail(getTextValue(primaryContact, "emailaddress1"));
        }
        
        return customer;
    }

    private QuoteDto mapToQuoteDto(JsonNode quote) {
        QuoteDto dto = new QuoteDto();
        dto.setQuoteId(getTextValue(quote, "quoteid"));
        dto.setQuoteNumber(getTextValue(quote, "quotenumber"));
        dto.setName(getTextValue(quote, "name"));
        dto.setTotalAmount(quote.has("totalamount") ? quote.get("totalamount").asDouble() : 0);
        dto.setStateCode(quote.has("statecode") ? quote.get("statecode").asInt() : 0);
        dto.setStatusCode(quote.has("statuscode") ? quote.get("statuscode").asInt() : 0);
        
        JsonNode account = quote.get("customerid_account");
        if (account != null) {
            dto.setCustomerName(getTextValue(account, "name"));
        }
        
        return dto;
    }

    private ComplaintDto mapToComplaintDto(JsonNode caseNode) {
        ComplaintDto dto = new ComplaintDto();
        dto.setComplaintId(getTextValue(caseNode, "incidentid"));
        dto.setTitle(getTextValue(caseNode, "title"));
        dto.setDescription(getTextValue(caseNode, "description"));
        dto.setStateCode(caseNode.has("statecode") ? caseNode.get("statecode").asInt() : 0);
        dto.setStatusCode(caseNode.has("statuscode") ? caseNode.get("statuscode").asInt() : 0);
        return dto;
    }

    private SalesOrderDto mapToSalesOrderDto(JsonNode order) {
        SalesOrderDto dto = new SalesOrderDto();
        dto.setOrderId(getTextValue(order, "salesorderid"));
        dto.setOrderNumber(getTextValue(order, "ordernumber"));
        dto.setName(getTextValue(order, "name"));
        dto.setTotalAmount(order.has("totalamount") ? order.get("totalamount").asDouble() : 0);
        dto.setRequestDeliveryBy(getTextValue(order, "requestdeliveryby"));
        return dto;
    }

    private String getTextValue(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }
}

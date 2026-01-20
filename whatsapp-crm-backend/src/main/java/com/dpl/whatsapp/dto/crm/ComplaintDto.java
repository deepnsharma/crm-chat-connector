package com.dpl.whatsapp.dto.crm;

import lombok.Data;
import java.util.Map;

@Data
public class ComplaintDto {
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

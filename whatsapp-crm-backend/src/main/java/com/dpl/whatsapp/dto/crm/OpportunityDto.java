package com.dpl.whatsapp.dto.crm;

import lombok.Data;
import java.util.Map;

@Data
public class OpportunityDto {
    private String name;
    private String description;
    private Double estimatedValue;
    private String accountId;
    private String contactId;
    private Map<String, Object> customFields;
}

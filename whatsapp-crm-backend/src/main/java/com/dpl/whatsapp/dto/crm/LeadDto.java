package com.dpl.whatsapp.dto.crm;

import lombok.Data;
import java.util.Map;

@Data
public class LeadDto {
    private String subject;
    private String firstName;
    private String lastName;
    private String phone;
    private String email;
    private String companyName;
    private String description;
    private Map<String, Object> customFields;
}

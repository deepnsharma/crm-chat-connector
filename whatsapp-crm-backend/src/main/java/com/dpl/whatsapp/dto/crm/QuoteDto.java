package com.dpl.whatsapp.dto.crm;

import lombok.Data;

@Data
public class QuoteDto {
    private String quoteId;
    private String quoteNumber;
    private String name;
    private double totalAmount;
    private int stateCode;
    private int statusCode;
    private String customerName;
}

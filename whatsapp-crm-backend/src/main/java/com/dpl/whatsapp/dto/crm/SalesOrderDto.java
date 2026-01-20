package com.dpl.whatsapp.dto.crm;

import lombok.Data;

@Data
public class SalesOrderDto {
    private String orderId;
    private String orderNumber;
    private String name;
    private double totalAmount;
    private String requestDeliveryBy;
}

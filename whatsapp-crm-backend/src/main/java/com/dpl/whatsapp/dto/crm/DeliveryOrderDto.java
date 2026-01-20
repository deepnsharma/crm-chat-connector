package com.dpl.whatsapp.dto.crm;

import lombok.Data;

@Data
public class DeliveryOrderDto {
    private String name;
    private String salesOrderId;
    private double quantity;
    private String deliveryDate;
    private String deliveryAddress;
    private String remarks;
    private String accountId;
}

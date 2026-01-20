package com.dpl.whatsapp.dto.whatsapp;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TemplateParameterDto {
    private String type;
    private String value;
    private String filename;
}

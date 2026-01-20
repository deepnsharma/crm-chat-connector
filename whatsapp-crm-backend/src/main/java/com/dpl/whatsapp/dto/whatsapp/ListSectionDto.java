package com.dpl.whatsapp.dto.whatsapp;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ListSectionDto {
    private String title;
    private List<ListRowDto> rows;
}

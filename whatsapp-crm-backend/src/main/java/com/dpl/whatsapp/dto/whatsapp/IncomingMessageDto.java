package com.dpl.whatsapp.dto.whatsapp;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
public class IncomingMessageDto {
    private String messageId;
    private String from;
    private String timestamp;
    private String type;
    private String text;
    private String buttonReplyId;
    private String buttonReplyTitle;
    private String listReplyId;
    private String listReplyTitle;
    private String profileName;
    private String mediaId;
    private Double latitude;
    private Double longitude;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ButtonDto {
    private String id;
    private String title;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ListSectionDto {
    private String title;
    private List<ListRowDto> rows;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class ListRowDto {
    private String id;
    private String title;
    private String description;
}

@Data
class MessageResponse {
    private boolean success;
    private String messageId;
    private String error;
}

@Data
class TemplateComponentDto {
    private String type;
    private List<TemplateParameterDto> parameters;
}

@Data
class TemplateParameterDto {
    private String type;
    private String value;
    private String filename;
}

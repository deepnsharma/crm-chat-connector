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

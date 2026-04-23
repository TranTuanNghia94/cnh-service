package com.cnh.ies.model.notification;

import java.util.List;

import lombok.Data;

@Data
public class CreateNotificationRequest {

    private List<String> userIds;
    
    private String title;
    
    private String message;
    
    private String type;
    
    private String category;
    
    private String referenceId;
    
    private String referenceType;
    
    private String actionUrl;
    
    private String priority;
    
    private String metadata;
}

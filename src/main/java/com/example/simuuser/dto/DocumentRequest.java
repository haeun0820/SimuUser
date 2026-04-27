package com.example.simuuser.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DocumentRequest { 
    private Long projectId;
    private String documentType;
    private String title;
    private String description;
}
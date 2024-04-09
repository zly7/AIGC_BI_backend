package com.yupi.springbootinit.model.dto.chat;

import lombok.Data;

@Data
public class CreateAIAssistantResponse {
    private String id;
    private String name;
    private String model;
}

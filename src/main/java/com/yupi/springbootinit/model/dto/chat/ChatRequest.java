package com.yupi.springbootinit.model.dto.chat;

import com.yupi.springbootinit.model.entity.Message;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {

    public String getModel() {
        return model;
    }

    private final String model;

    public List<Message> getMessages() {
        return messages;
    }

    private final List<Message> messages;
    private int n;
    private double temperature;

    // Default constructor

    public ChatRequest(String model, String prompt) {
        this.model = model;
        this.messages = new ArrayList<>();
        this.messages.add(new Message("user", prompt));
    }

    // Getters and setters
}

package com.yupi.springbootinit.model.entity;

import lombok.Getter;

@Getter
public class Message {

    private final String role;
    private final String content;

    public Message(String user, String prompt) {
        this.role = user;
        this.content = prompt;
    }

    // constructor, getters and setters

}

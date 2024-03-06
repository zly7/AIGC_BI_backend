package com.yupi.springbootinit.model.dto.chat;
import com.yupi.springbootinit.model.entity.Message;
import lombok.Getter;

import java.util.List;

public class ChatResponse {

    private final List<Choice> choices;

    public ChatResponse(List<Choice> choices) {
        this.choices = choices;
    }

    // constructors, getters and setters
    public List<Choice> getChoices() {
        return choices;
    }


    public static class Choice {

        private final int index;

        @Getter
        private final  Message message;

        public Choice(int index, Message message) {
            this.index = index;
            this.message = message;
        }

        // constructors, getters and setters

    }
}


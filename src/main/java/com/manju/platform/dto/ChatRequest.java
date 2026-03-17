package com.manju.platform.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChatRequest {
    private String model;
    private List<Message> messages;
    private Thinking thinking;
    private int max_tokens;
    private double temperature;

    public ChatRequest(String model, List<Message> messages, Thinking thinking, int max_tokens, double temperature) {
        this.model = model;
        this.messages = messages;
        this.thinking = thinking;
        this.max_tokens = max_tokens;
        this.temperature = temperature;
    }
}

package com.manju.platform.dto;

import lombok.Data;

@Data
public class Thinking {
    private String type;

    public Thinking(String type) {
        this.type = type;
    }
}

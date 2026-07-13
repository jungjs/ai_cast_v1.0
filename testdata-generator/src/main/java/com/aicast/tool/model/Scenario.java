package com.aicast.tool.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Scenario {
    private int index;
    private String title;
    private String body;
    private String ttsCheckpoint;

    public String getSafeFileName() {
        return String.format("%02d_%s", index, title.replaceAll("[^가-힣a-zA-Z0-9\\s]", "").trim().replaceAll("\\s+", "_"));
    }
}

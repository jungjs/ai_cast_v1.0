package com.aicast.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponseDto {
    private List<Map<String, Object>> aiStats;
    private List<Map<String, Object>> apiStats;
    private List<Map<String, Object>> trendStats;
}

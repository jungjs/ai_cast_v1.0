package com.aicast.controller;

import com.aicast.service.log.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    @GetMapping("/daily")
    public ResponseEntity<List<Map<String, Object>>> getDailyStats(
            @RequestParam("govId") String govId,
            @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(statsService.getDailyStats(govId, date));
    }

    @GetMapping("/weekly")
    public ResponseEntity<List<Map<String, Object>>> getWeeklyStats(
            @RequestParam("govId") String govId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statsService.getWeeklyStats(govId, startDate, endDate));
    }

    @GetMapping("/monthly")
    public ResponseEntity<List<Map<String, Object>>> getMonthlyStats(
            @RequestParam("govId") String govId,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(statsService.getMonthlyStats(govId, startDate, endDate));
    }

    @PostMapping("/aggregate/today")
    public ResponseEntity<String> forceAggregateToday() {
        statsService.aggregateTodayStats();
        return ResponseEntity.ok("Successfully aggregated today's stats!");
    }
}

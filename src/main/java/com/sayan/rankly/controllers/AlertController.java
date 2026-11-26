package com.sayan.rankly.controllers;

import com.sayan.rankly.dto.alert.AlertResponse;
import com.sayan.rankly.dto.alert.UnreadCountResponse;
import com.sayan.rankly.services.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<List<AlertResponse>> listAlerts(
            Authentication authentication,
            @RequestParam(required = false) Long domainId,
            @RequestParam(defaultValue = "false") boolean unreadOnly
    ) {
        return ResponseEntity.ok(alertService.getAlerts(authentication, domainId, unreadOnly));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(Authentication authentication) {
        return ResponseEntity.ok(alertService.getUnreadCount(authentication));
    }

    @PatchMapping("/{alertId}/read")
    public ResponseEntity<AlertResponse> markAlertAsRead(
            Authentication authentication,
            @PathVariable Long alertId
    ) {
        return ResponseEntity.ok(alertService.markAsRead(authentication, alertId));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAlertsAsRead(
            Authentication authentication,
            @RequestParam(required = false) Long domainId
    ) {
        int updatedCount = alertService.markAllAsRead(authentication, domainId);
        return ResponseEntity.ok(Map.of("updatedCount", updatedCount));
    }
}

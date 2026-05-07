package com.zym.ecart.controller;

import com.zym.ecart.scheduler.DailyOrderReportScheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

/**
 * Admin-only controller for manually triggering the daily order report.
 * Protected by Spring Security — requires ROLE_ADMIN (via /admin/** rule).
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Testing the report flow locally or on EC2</li>
 *   <li>Re-generating a missed report</li>
 *   <li>Generating a report for a specific past date</li>
 * </ul>
 */
@RestController
@RequestMapping("/admin/reports")
public class ReportController {

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DailyOrderReportScheduler reportScheduler;

    public ReportController(DailyOrderReportScheduler reportScheduler) {
        this.reportScheduler = reportScheduler;
    }

    /**
     * Trigger report for yesterday (IST).
     * 
     * POST /admin/reports/daily/trigger
     */
    @PostMapping("/daily/trigger")
    public ResponseEntity<Map<String, Object>> triggerYesterdayReport() {
        LocalDate yesterday = LocalDate.now(IST).minusDays(1);
        log.info("🔧 Admin triggered daily report for yesterday: {}", yesterday);

        reportScheduler.triggerManualReport(yesterday);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Daily report triggered for " + yesterday,
                "reportDate", yesterday.toString()
        ));
    }

    /**
     * Trigger report for a specific IST date.
     *
     * POST /admin/reports/daily/trigger/{date}
     * Example: POST /admin/reports/daily/trigger/2026-04-28
     */
    @PostMapping("/daily/trigger/{date}")
    public ResponseEntity<Map<String, Object>> triggerReportForDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("🔧 Admin triggered daily report for specific date: {}", date);

        reportScheduler.triggerManualReport(date);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Daily report triggered for " + date,
                "reportDate", date.toString()
        ));
    }

    /**
     * Trigger report for TODAY (useful for testing during development).
     *
     * POST /admin/reports/daily/trigger-today
     */
    @PostMapping("/daily/trigger-today")
    public ResponseEntity<Map<String, Object>> triggerTodayReport() {
        LocalDate today = LocalDate.now(IST);
        log.info("🔧 Admin triggered daily report for today: {}", today);

        reportScheduler.triggerManualReport(today);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Daily report triggered for today: " + today,
                "reportDate", today.toString()
        ));
    }

    // ──────────────── DAILY MAIL TOGGLE ────────────────

    /**
     * Get the current status of the daily mail toggle.
     *
     * GET /admin/reports/daily-mail/status
     */
    @GetMapping("/daily-mail/status")
    public ResponseEntity<Map<String, Object>> getDailyMailStatus() {
        boolean enabled = reportScheduler.isDailyMailEnabled();
        return ResponseEntity.ok(Map.of(
                "enabled", enabled,
                "message", "Daily mail is " + (enabled ? "ENABLED" : "DISABLED")
        ));
    }

    /**
     * Toggle the daily mail on/off.
     *
     * POST /admin/reports/daily-mail/toggle
     * Body: { "enabled": true } or { "enabled": false }
     */
    @PostMapping("/daily-mail/toggle")
    public ResponseEntity<Map<String, Object>> toggleDailyMail(@RequestBody Map<String, Boolean> request) {
        Boolean enabled = request.get("enabled");
        if (enabled == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "enabled field is required"));
        }

        reportScheduler.setDailyMailEnabled(enabled);

        return ResponseEntity.ok(Map.of(
                "status", "success",
                "enabled", enabled,
                "message", "Daily mail " + (enabled ? "ENABLED" : "DISABLED") + " successfully"
        ));
    }
}

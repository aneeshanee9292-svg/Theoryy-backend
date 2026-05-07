package com.zym.ecart.scheduler;

import com.zym.ecart.service.impl.DailyOrderReportService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Scheduler that triggers the daily order report at 00:10 IST.
 *
 * <p><b>Why 00:10 IST and not 00:00?</b></p>
 * <ul>
 *   <li>Gives a 10-minute buffer for any last-second orders at 23:59 IST</li>
 *   <li>Avoids race conditions with midnight-boundary orders</li>
 *   <li>Ensures DB writes from 23:59 IST orders are fully committed</li>
 * </ul>
 *
 * <p><b>Timezone Note:</b></p>
 * The cron uses {@code zone = "Asia/Kolkata"} so it fires at IST 00:10
 * regardless of the EC2 instance's system timezone (UTC in eu-north-1).
 * Without the zone parameter, the cron would use the JVM's default timezone.
 */
@Component
public class DailyOrderReportScheduler {

    private static final Logger log = LoggerFactory.getLogger(DailyOrderReportScheduler.class);

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    private final DailyOrderReportService reportService;

    /**
     * Toggle to enable/disable the daily cron mail.
     * Default is enabled (true). Admin can toggle via API.
     */
    private final AtomicBoolean dailyMailEnabled = new AtomicBoolean(true);

    public DailyOrderReportScheduler(DailyOrderReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * Runs every day at 00:10 IST.
     *
     * Cron expression: second=0, minute=10, hour=0, day=*, month=*, weekday=*
     * Zone explicitly set to IST so EC2 (UTC) fires at the correct IST time.
     *
     * Reports on the PREVIOUS day's orders (yesterday IST).
     */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Kolkata")
    public void runDailyReport() {
        if (!dailyMailEnabled.get()) {
            log.info("⏸️ Daily mail is DISABLED. Skipping report.");
            return;
        }

        LocalDate yesterday = LocalDate.now(IST).minusDays(1);

        log.info("⏰ Daily order report scheduler triggered. Reporting for IST date: {}", yesterday);

        try {
            reportService.generateAndSendReport(yesterday);
            log.info("✅ Daily order report completed successfully for IST date: {}", yesterday);
        } catch (Exception e) {
            // CRITICAL: Must not crash the application
            log.error("❌ Daily order report FAILED for IST date {}: {}", yesterday, e.getMessage(), e);
        }
    }

    /**
     * Public method to allow manual triggering (e.g., from a controller or test).
     * Generates a report for any given IST date.
     *
     * @param istDate the IST date to generate the report for
     */
    public void triggerManualReport(LocalDate istDate) {
        log.info("🔧 Manual report trigger for IST date: {}", istDate);
        try {
            reportService.generateAndSendReport(istDate);
            log.info("✅ Manual report completed for IST date: {}", istDate);
        } catch (Exception e) {
            log.error("❌ Manual report FAILED for IST date {}: {}", istDate, e.getMessage(), e);
            throw new RuntimeException("Report generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Check if daily mail is enabled.
     */
    public boolean isDailyMailEnabled() {
        return dailyMailEnabled.get();
    }

    /**
     * Enable or disable the daily mail.
     */
    public void setDailyMailEnabled(boolean enabled) {
        dailyMailEnabled.set(enabled);
        log.info("📧 Daily mail {} by admin", enabled ? "ENABLED" : "DISABLED");
    }
}

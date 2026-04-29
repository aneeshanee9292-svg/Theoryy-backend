package com.zym.ecart.service.impl;

import com.zym.ecart.entity.Order;
import com.zym.ecart.repository.OrderRepository;

import jakarta.mail.internet.MimeMessage;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Service responsible for generating the daily orders Excel report
 * and sending it as an email attachment to the admin.
 *
 * <p><b>Timezone Strategy:</b></p>
 * <ul>
 *   <li>Business day = IST (Asia/Kolkata) 00:00 to 23:59:59.999</li>
 *   <li>DB stores timestamps as UTC (LocalDateTime)</li>
 *   <li>We convert IST day boundaries → UTC before querying the DB</li>
 *   <li>EC2 in eu-north-1 runs on UTC, so no JVM timezone dependency</li>
 * </ul>
 *
 * <p><b>Memory Strategy:</b></p>
 * <ul>
 *   <li>Uses Apache POI SXSSFWorkbook (streaming) — only keeps 100 rows in memory</li>
 *   <li>Fetches orders from DB in pages of 500</li>
 * </ul>
 */
@Service
public class DailyOrderReportService {

    private static final Logger log = LoggerFactory.getLogger(DailyOrderReportService.class);

    /** IST timezone — all business logic uses this */
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /** UTC timezone — DB timestamps are stored in UTC */
    private static final ZoneId UTC = ZoneId.of("UTC");

    /** Number of orders to fetch per DB query */
    private static final int BATCH_SIZE = 500;

    /** Admin email — same one used for order notifications in EmailServiceImpl */
    private static final String ADMIN_EMAIL = "contact@theoryy.info";

    private static final DateTimeFormatter IST_DATE_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DateTimeFormatter IST_DATETIME_FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");

    /** Excel column headers */
    private static final String[] HEADERS = {
            "Order ID", "Customer Name", "Email", "Phone",
            "Address Line", "City", "State", "Pincode", "Full Address",
            "Total Amount", "Status", "Created At (IST)"
    };

    private final OrderRepository orderRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public DailyOrderReportService(OrderRepository orderRepository,
                                   JavaMailSender mailSender) {
        this.orderRepository = orderRepository;
        this.mailSender = mailSender;
    }

    /**
     * Main entry point — generates the report for the given IST date and emails it.
     *
     * @param reportDate the IST date to generate the report for (typically yesterday)
     */
    public void generateAndSendReport(LocalDate reportDate) {
        log.info("📊 Starting daily order report for IST date: {}", reportDate);

        // ────────────────────────────────────────────────────────
        // Step 1: Convert IST day boundaries to UTC for DB query
        // ────────────────────────────────────────────────────────
        // IST 00:00:00 of reportDate → UTC = reportDate minus 5:30 hours
        // IST 00:00:00 of next day  → UTC = next day minus 5:30 hours
        //
        // Example: IST 2026-04-28 00:00 = UTC 2026-04-27 18:30
        //          IST 2026-04-29 00:00 = UTC 2026-04-28 18:30
        LocalDateTime utcStart = reportDate.atStartOfDay(IST)
                .withZoneSameInstant(UTC)
                .toLocalDateTime();

        LocalDateTime utcEnd = reportDate.plusDays(1).atStartOfDay(IST)
                .withZoneSameInstant(UTC)
                .toLocalDateTime();

        log.info("  IST range: {} 00:00 — {} 23:59:59", reportDate, reportDate);
        log.info("  UTC range: {} — {}", utcStart, utcEnd);

        // ────────────────────────────────────────────────────────
        // Step 2: Quick count — skip if no orders
        // ────────────────────────────────────────────────────────
        long totalOrders = orderRepository.countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(utcStart, utcEnd);

        if (totalOrders == 0) {
            log.info("📭 No orders found for IST date {}. Skipping report.", reportDate);
            sendNoOrdersEmail(reportDate);
            return;
        }

        log.info("  Found {} orders for IST date {}", totalOrders, reportDate);

        // ────────────────────────────────────────────────────────
        // Step 3: Generate Excel using streaming workbook
        // ────────────────────────────────────────────────────────
        byte[] excelBytes;
        double totalRevenue = 0;
        long rowCount = 0;

        // SXSSFWorkbook keeps only 100 rows in memory (rest flushed to temp files)
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            Sheet sheet = workbook.createSheet("Orders - " + reportDate.format(IST_DATE_FMT));

            // -- Create header style --
            CellStyle headerStyle = createHeaderStyle(workbook);

            // -- Write header row --
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // -- Create amount cell style (₹ format) --
            CellStyle amountStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            amountStyle.setDataFormat(format.getFormat("₹#,##0.00"));

            // -- Create date cell style --
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd-mmm-yyyy hh:mm AM/PM"));

            // ─── Paginated fetch & write ───
            int currentRow = 1;
            int pageNumber = 0;
            Page<Order> page;

            do {
                Pageable pageable = PageRequest.of(pageNumber, BATCH_SIZE);
                page = orderRepository.findOrdersByCreatedAtBetween(utcStart, utcEnd, pageable);

                for (Order order : page.getContent()) {
                    Row row = sheet.createRow(currentRow++);
                    totalRevenue += writeOrderRow(row, order, amountStyle, dateStyle);
                    rowCount++;
                }

                pageNumber++;
                log.debug("  Wrote page {} ({} orders so far)", pageNumber, rowCount);

            } while (page.hasNext());

            // -- Freeze header row + auto-filter --
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                    0, 0, 0, HEADERS.length - 1));

            // -- Set column widths (approximate) --
            // Note: SXSSFWorkbook doesn't support autoSizeColumn,
            // so we set reasonable fixed widths
            int[] colWidths = {
                    3000,  // Order ID
                    6000,  // Customer Name
                    8000,  // Email
                    4500,  // Phone
                    8000,  // Address Line
                    4000,  // City
                    4000,  // State
                    3000,  // Pincode
                    12000, // Full Address
                    4000,  // Total Amount
                    4500,  // Status
                    6000   // Created At
            };
            for (int i = 0; i < colWidths.length; i++) {
                sheet.setColumnWidth(i, colWidths[i]);
            }

            // -- Write to byte array --
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            excelBytes = bos.toByteArray();

            // Dispose temp files created by SXSSF
            workbook.dispose();

        } catch (Exception e) {
            log.error("❌ Failed to generate Excel for IST date {}: {}", reportDate, e.getMessage(), e);
            return;
        }

        // ────────────────────────────────────────────────────────
        // Step 4: Send email with attachment
        // ────────────────────────────────────────────────────────
        sendReportEmail(reportDate, excelBytes, rowCount, totalRevenue);
    }

    /**
     * Writes a single order to an Excel row. Returns the finalAmount for revenue summing.
     */
    private double writeOrderRow(Row row, Order order, CellStyle amountStyle, CellStyle dateStyle) {
        int col = 0;

        // Order ID
        row.createCell(col++).setCellValue(order.getId() != null ? order.getId() : 0);

        // Customer Name
        row.createCell(col++).setCellValue(safe(order.getFullName()));

        // Email
        row.createCell(col++).setCellValue(safe(order.getEmail()));

        // Phone
        row.createCell(col++).setCellValue(safe(order.getMobileNumber()));

        // Address Line
        row.createCell(col++).setCellValue(safe(order.getAddress()));

        // City
        row.createCell(col++).setCellValue(safe(order.getCity()));

        // State
        row.createCell(col++).setCellValue(safe(order.getState()));

        // Pincode
        row.createCell(col++).setCellValue(safe(order.getPincode()));

        // Full Address (combined)
        String fullAddress = String.join(", ",
                safe(order.getAddress()),
                safe(order.getCity()),
                safe(order.getState()),
                safe(order.getPincode())
        ).replaceAll(", ,", ",").replaceAll("^, |, $", "");
        row.createCell(col++).setCellValue(fullAddress);

        // Total Amount (finalAmount = after discounts)
        double amount = order.getFinalAmount() != null ? order.getFinalAmount() : 0.0;
        Cell amountCell = row.createCell(col++);
        amountCell.setCellValue(amount);
        amountCell.setCellStyle(amountStyle);

        // Status
        row.createCell(col++).setCellValue(
                order.getStatus() != null ? order.getStatus().name() : "UNKNOWN");

        // Created At — convert UTC → IST for display
        if (order.getCreatedAt() != null) {
            ZonedDateTime istTime = order.getCreatedAt().atZone(UTC).withZoneSameInstant(IST);
            row.createCell(col).setCellValue(istTime.format(IST_DATETIME_FMT));
        } else {
            row.createCell(col).setCellValue("N/A");
        }

        return amount;
    }

    /**
     * Creates a bold, colored header style for the Excel sheet.
     */
    private CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 11);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);

        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        style.setAlignment(HorizontalAlignment.CENTER);

        return style;
    }

    /**
     * Sends the Excel report as an email attachment to the admin.
     */
    private void sendReportEmail(LocalDate reportDate, byte[] excelBytes, long orderCount, double totalRevenue) {
        try {
            String formattedDate = reportDate.format(IST_DATE_FMT);
            String subject = "Daily Orders Report - " + formattedDate;
            String fileName = "orders-report-" + reportDate + ".xlsx";

            String body = buildReportEmailBody(formattedDate, orderCount, totalRevenue);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(ADMIN_EMAIL);
            helper.setSubject(subject);
            helper.setText(body, true); // HTML body

            helper.addAttachment(fileName, new ByteArrayResource(excelBytes),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

            mailSender.send(mimeMessage);

            log.info("✅ Daily report email sent for IST date {}. Orders: {}, Revenue: ₹{}", 
                    reportDate, orderCount, String.format("%.2f", totalRevenue));

        } catch (Exception e) {
            log.error("❌ Failed to send daily report email for IST date {}: {}", reportDate, e.getMessage(), e);
        }
    }

    /**
     * Sends a notification email when there are no orders for the day.
     */
    private void sendNoOrdersEmail(LocalDate reportDate) {
        try {
            String formattedDate = reportDate.format(IST_DATE_FMT);
            String subject = "Daily Orders Report - " + formattedDate + " (No Orders)";

            String body = "<!DOCTYPE html><html><body style='font-family:Segoe UI,Arial,sans-serif;padding:20px;'>"
                    + "<div style='max-width:500px;margin:0 auto;background:#fff;border-radius:12px;padding:30px;"
                    + "box-shadow:0 4px 15px rgba(0,0,0,0.08);text-align:center;'>"
                    + "<h2 style='color:#1a1a1a;'>📭 No Orders Received</h2>"
                    + "<p style='color:#666;font-size:15px;'>There were no orders placed on <strong>" 
                    + formattedDate + "</strong> (IST).</p>"
                    + "<hr style='border:none;border-top:1px solid #eee;margin:20px 0;'>"
                    + "<p style='color:#999;font-size:12px;'>This is an automated report from THEORYY.</p>"
                    + "</div></body></html>";

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(ADMIN_EMAIL);
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(mimeMessage);

            log.info("📭 No-orders notification email sent for IST date {}", reportDate);

        } catch (Exception e) {
            log.error("❌ Failed to send no-orders email for IST date {}: {}", reportDate, e.getMessage(), e);
        }
    }

    /**
     * Builds a styled HTML email body with order summary statistics.
     */
    private String buildReportEmailBody(String formattedDate, long orderCount, double totalRevenue) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Segoe UI,Arial,sans-serif;background:#f8f9fa;margin:0;padding:20px;'>"
                + "<div style='max-width:550px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;"
                + "box-shadow:0 4px 20px rgba(0,0,0,0.08);'>"
                // Header
                + "<div style='background:linear-gradient(135deg,#1a1a1a,#2d2d2d);color:#fff;"
                + "text-align:center;padding:30px;'>"
                + "<h1 style='margin:0;font-size:22px;letter-spacing:2px;'>📊 DAILY ORDERS REPORT</h1>"
                + "<p style='margin:8px 0 0;font-size:14px;opacity:0.85;'>" + formattedDate + " (IST)</p>"
                + "</div>"
                // Stats
                + "<div style='padding:30px;text-align:center;'>"
                + "<div style='display:inline-block;margin:0 20px;'>"
                + "<div style='font-size:36px;font-weight:700;color:#1a1a1a;'>" + orderCount + "</div>"
                + "<div style='font-size:12px;color:#888;text-transform:uppercase;letter-spacing:1px;'>Total Orders</div>"
                + "</div>"
                + "<div style='display:inline-block;margin:0 20px;'>"
                + "<div style='font-size:36px;font-weight:700;color:#4caf50;'>₹" 
                + String.format("%,.2f", totalRevenue) + "</div>"
                + "<div style='font-size:12px;color:#888;text-transform:uppercase;letter-spacing:1px;'>Total Revenue</div>"
                + "</div>"
                + "</div>"
                // Note
                + "<div style='background:#f5f5f5;padding:20px 30px;text-align:center;'>"
                + "<p style='margin:0;color:#666;font-size:13px;'>"
                + "📎 The detailed Excel report is attached to this email.</p>"
                + "</div>"
                // Footer
                + "<div style='background:#1a1a1a;color:#aaa;text-align:center;padding:20px;font-size:11px;'>"
                + "This is an automated report from THEORYY &bull; Powered by Spring Boot"
                + "</div>"
                + "</div></body></html>";
    }

    /** Null-safe string helper */
    private String safe(String value) {
        return value != null ? value : "";
    }
}

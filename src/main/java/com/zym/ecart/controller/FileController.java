package com.zym.ecart.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.service.S3StorageService;

@RestController
@RequestMapping("/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    private final S3StorageService s3StorageService;

    public FileController(S3StorageService s3StorageService) {
        this.s3StorageService = s3StorageService;
    }

    /**
     * Get a pre-signed URL for direct browser-to-S3 upload.
     * GET /files/presign?filename=image.png&contentType=image/png
     */
    @GetMapping("/presign")
    public ResponseEntity<ApiResponse<Map<String, String>>> getPresignedUrl(
            @RequestParam String filename,
            @RequestParam String contentType) {
        try {
            logger.info("Pre-sign request: filename={}, contentType={}", filename, contentType);

            if (!contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Only image files are allowed", null));
            }

            String[] urls = s3StorageService.generatePresignedUploadUrl(filename, contentType);

            Map<String, String> result = Map.of(
                    "uploadUrl", urls[0],
                    "publicUrl", urls[1]
            );

            return ResponseEntity.ok(new ApiResponse<>(true, "Pre-signed URL generated", result));

        } catch (Exception e) {
            logger.error("Failed to generate pre-signed URL", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed: " + e.getMessage(), null));
        }
    }

    /**
     * Test S3 connectivity — uploads a tiny test file.
     * GET /files/test-upload
     */
    @GetMapping("/test-upload")
    public ResponseEntity<ApiResponse<String>> testUpload() {
        try {
            logger.info("=== TEST UPLOAD START ===");
            String key = "gallery/test_" + System.currentTimeMillis() + ".txt";
            s3StorageService.uploadTestFile(key, "S3 connectivity test");
            logger.info("=== TEST UPLOAD SUCCESS ===");
            return ResponseEntity.ok(new ApiResponse<>(true, "S3 test upload works!", key));
        } catch (Exception e) {
            logger.error("=== TEST UPLOAD FAILED ===", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "S3 test failed: " + e.getMessage(), null));
        }
    }

    /**
     * List all images in the S3 gallery.
     * GET /files/list
     */
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<String>>> listFiles() {
        try {
            List<String> fileUrls = s3StorageService.listFiles();
            return ResponseEntity.ok(new ApiResponse<>(true, "Files listed", fileUrls));

        } catch (Exception e) {
            logger.error("Failed to list files", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to list files: " + e.getMessage(), null));
        }
    }

    /**
     * Delete an image from S3 gallery.
     * DELETE /files/{filename}
     */
    @DeleteMapping("/{filename}")
    public ResponseEntity<ApiResponse<String>> deleteFile(@PathVariable String filename) {
        try {
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid filename", null));
            }

            s3StorageService.deleteFile(filename);

            return ResponseEntity.ok(new ApiResponse<>(true, "File deleted", filename));

        } catch (Exception e) {
            logger.error("Failed to delete file", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to delete file: " + e.getMessage(), null));
        }
    }
}

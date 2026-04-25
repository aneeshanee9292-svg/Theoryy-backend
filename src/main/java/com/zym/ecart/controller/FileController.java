package com.zym.ecart.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
     * Upload an image to S3 gallery folder.
     * POST /files/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @RequestParam("file") MultipartFile file) {

        try {
            logger.info("Upload request received: {}", file.getOriginalFilename());

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "File is empty", null));
            }

            // Validate file size (5 MB max)
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "File size exceeds 5 MB limit", null));
            }

            // Validate content type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Only image files are allowed", null));
            }

            String fileUrl = s3StorageService.uploadFile(file);

            return ResponseEntity.ok(new ApiResponse<>(true, "File uploaded", fileUrl));

        } catch (Exception e) {
            logger.error("File upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "File upload failed: " + e.getMessage(), null));
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
            // Sanitize filename
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

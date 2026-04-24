package com.zym.ecart.controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.nio.file.Files;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zym.ecart.dto.ApiResponse;

@RestController
@RequestMapping("/files")
@CrossOrigin
public class FileController {

    @Value("${file.upload-dir}")
    private String uploadDir;

    // Allowed folders
    private static final List<String> ALLOWED_FOLDERS = List.of("products", "banners", "profile");

    @PostMapping("/upload/{folder}")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @PathVariable String folder,
            @RequestParam("file") MultipartFile file) {

        try {
            // Validate folder
            if (!ALLOWED_FOLDERS.contains(folder)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid folder name", null));
            }

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "File is empty", null));
            }

            // Unique filename
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, folder, fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            // Build URL using /uploads/ path that WebConfig serves
            String fileUrl = "http://localhost:8081/uploads/" + folder + "/" + fileName;

            return ResponseEntity.ok(new ApiResponse<>(true, "File uploaded", fileUrl));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "File upload failed", null));
        }
    }
        
        
    @GetMapping("/list/{folder}")
    public ResponseEntity<ApiResponse<List<String>>> listFiles(@PathVariable String folder) {
        try {
            Path folderPath = Paths.get(uploadDir, folder);

            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Folder not found", null));
            }

            // Collect all file URLs using the /uploads/ path
            List<String> fileUrls = Files.list(folderPath)
                    .filter(Files::isRegularFile)
                    .map(path -> "http://localhost:8081/uploads/" + folder + "/" + path.getFileName().toString())
                    .toList();

            return ResponseEntity.ok(new ApiResponse<>(true, "Files listed", fileUrls));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to list files", null));
        }
    }

    @DeleteMapping("/{folder}/{filename}")
    public ResponseEntity<ApiResponse<String>> deleteFile(
            @PathVariable String folder,
            @PathVariable String filename) {

        try {
            // Validate folder
            if (!ALLOWED_FOLDERS.contains(folder)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid folder name", null));
            }

            // Sanitize filename to prevent path traversal
            if (filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Invalid filename", null));
            }

            Path filePath = Paths.get(uploadDir, folder, filename);

            if (!Files.exists(filePath)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "File not found", null));
            }

            Files.delete(filePath);

            return ResponseEntity.ok(new ApiResponse<>(true, "File deleted", filename));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "Failed to delete file: " + e.getMessage(), null));
        }
    }

}

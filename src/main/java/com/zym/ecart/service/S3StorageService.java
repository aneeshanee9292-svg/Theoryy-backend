package com.zym.ecart.service;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

@Service
public class S3StorageService {

    private static final Logger logger = LoggerFactory.getLogger(S3StorageService.class);

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    @Value("${aws.s3.region}")
    private String region;

    @Value("${aws.s3.gallery-folder:gallery}")
    private String galleryFolder;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Upload a file to S3 under the gallery folder.
     * Returns the public S3 URL.
     */
    public String uploadFile(MultipartFile file) throws IOException {
        // Generate unique filename to avoid collisions
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = galleryFolder + "/" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

        logger.info("Uploading file to S3: bucket={}, key={}", bucketName, key);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(file.getBytes()));

        String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, key);
        logger.info("File uploaded successfully: {}", fileUrl);

        return fileUrl;
    }

    /**
     * List all files in the gallery folder.
     * Returns list of public S3 URLs.
     */
    public List<String> listFiles() {
        ListObjectsV2Request listRequest = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(galleryFolder + "/")
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(listRequest);

        return response.contents().stream()
                .filter(obj -> !obj.key().endsWith("/")) // skip folder markers
                .map(obj -> String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, obj.key()))
                .collect(Collectors.toList());
    }

    /**
     * Delete a file from S3 by its filename (just the filename part, not the full URL).
     */
    public void deleteFile(String filename) {
        String key = galleryFolder + "/" + filename;

        logger.info("Deleting file from S3: bucket={}, key={}", bucketName, key);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(deleteRequest);
        logger.info("File deleted successfully: {}", key);
    }
}

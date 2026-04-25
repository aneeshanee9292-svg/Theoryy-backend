package com.zym.ecart.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zym.ecart.dto.ApiResponse;
import com.zym.ecart.entity.Coupon;
import com.zym.ecart.repository.CouponRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/coupons")
public class CouponController {

    private final CouponRepository couponRepository;
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    public CouponController(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }
    @Value("${file.upload-dir}")
    private String uploadDir;

    /**
     * Public endpoint — validates a coupon code and returns discount preview.
     * Used by the frontend to show updated price before Razorpay is triggered.
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateCoupon(
            @RequestParam String code,
            @RequestParam double amount) {

        Optional<Coupon> optCoupon = couponRepository.findByCodeAndActiveTrue(code);

        if (optCoupon.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("valid", false, "message", "Invalid or inactive coupon code"));
        }

        Coupon coupon = optCoupon.get();

        // Check minimum order amount
        if (coupon.getMinOrderAmount() != null && amount < coupon.getMinOrderAmount()) {
            return ResponseEntity.badRequest().body(
                    java.util.Map.of("valid", false, "message",
                            "Minimum order amount is ₹" + coupon.getMinOrderAmount()));
        }

        double discount = 0;
        if ("PERCENTAGE".equals(coupon.getDiscountType())) {
            discount = amount * coupon.getDiscountValue() / 100;
            // Apply maxDiscount cap if set
            if (coupon.getMaxDiscount() != null && coupon.getMaxDiscount() > 0) {
                discount = Math.min(discount, coupon.getMaxDiscount());
            }
        } else {
            discount = coupon.getDiscountValue();
        }

        discount = Math.min(discount, amount); // can't discount more than order amount
        double finalAmount = amount - discount;

        return ResponseEntity.ok(java.util.Map.of(
                "valid", true,
                "discount", discount,
                "finalAmount", finalAmount,
                "discountType", coupon.getDiscountType(),
                "discountValue", coupon.getDiscountValue(),
                "message", "PERCENTAGE".equals(coupon.getDiscountType())
                        ? coupon.getDiscountValue() + "% off" +
                          (coupon.getMaxDiscount() != null && coupon.getMaxDiscount() > 0
                                  ? " (upto ₹" + coupon.getMaxDiscount() + ")"
                                  : "")
                        : "₹" + coupon.getDiscountValue() + " off"
        ));
        
    }
    
    

    // Allowed folders
    private static final List<String> ALLOWED_FOLDERS = List.of("products", "banners", "profile");

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443))
            return scheme + "://" + host;
        return scheme + "://" + host + ":" + port;
    }

    @PostMapping("/upload/{folder}")
    public ResponseEntity<ApiResponse<String>> uploadFile(
            @PathVariable String folder,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
    	

        try {
        	 logger.info("Upload request received for folder: {}", folder);

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

             // Validate uploads directory
             Path uploadsPath = Paths.get(uploadDir, folder);
             if (!Files.exists(uploadsPath) || !Files.isDirectory(uploadsPath)) {
                 return ResponseEntity.badRequest()
                         .body(new ApiResponse<>(false, "Uploads directory not accessible", null));
             }

             // Check if directory is empty
             boolean isEmpty = Files.list(uploadsPath).findAny().isEmpty();
             if (isEmpty) {
                 // ✅ Create test file if empty
                 String testFileName = "test_" + System.currentTimeMillis() + ".txt";
                 Path testFilePath = uploadsPath.resolve(testFileName);
                 Files.createDirectories(testFilePath.getParent());
                 Files.writeString(testFilePath, "This is a test file created automatically.");
                 logger.info("Test file created: {}", testFilePath.toString());
             } else {
                 // ✅ Directory accessible and has files
                 logger.info("Uploads directory accessible, existing files present in folder: {}", folder);
             }
        	
         
         // ✅ Validate file size
            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "File size exceeds 5 MB limit", null));
            }

            // Unique filename
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadDir, folder, fileName);

            Files.createDirectories(path.getParent());
            Files.write(path, file.getBytes());

            String fileUrl = getBaseUrl(request) + "/uploads/" + folder + "/" + fileName;

            return ResponseEntity.ok(new ApiResponse<>(true, "File uploaded", fileUrl));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new ApiResponse<>(false, "File upload failed", null));
        }
    }
        
        
    @GetMapping("/list/{folder}")
    public ResponseEntity<ApiResponse<List<String>>> listFiles(@PathVariable String folder, HttpServletRequest request) {
        try {
            Path folderPath = Paths.get(uploadDir, folder);

            if (!Files.exists(folderPath) || !Files.isDirectory(folderPath)) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(false, "Folder not found", null));
            }

            String baseUrl = getBaseUrl(request);
            List<String> fileUrls = Files.list(folderPath)
                    .filter(Files::isRegularFile)
                    .map(path -> baseUrl + "/uploads/" + folder + "/" + path.getFileName().toString())
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

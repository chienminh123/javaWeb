// src/main/java/com/example/demo/service/ImageService.java
package com.example.demo.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {
    private static final String UPLOAD_DIR = "src/main/resources/static/uploads/products/";

    public ImageService() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Không thể tạo thư mục upload!", e);
        }
    }


    public String saveSingleImage(MultipartFile file, Integer productId) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        try {

            String originalName = file.getOriginalFilename();
            String extension = originalName != null && originalName.contains(".")
                ? originalName.substring(originalName.lastIndexOf("."))
                : ".jpg";
            String fileName = productId + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;

            Path filePath = Paths.get(UPLOAD_DIR + fileName);
            Files.copy(file.getInputStream(), filePath);

            return "/uploads/products/" + fileName;

        } catch (IOException e) {
            throw new RuntimeException("Lưu ảnh thất bại: " + file.getOriginalFilename(), e);
        }
    }
}
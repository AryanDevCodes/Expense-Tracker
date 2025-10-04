package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.service.FileStorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Value("${app.file.upload-dir:uploads/receipts}")
    private String uploadDir;

    @Value("${app.file.base-url:http://localhost:8080/files}")
    private String baseUrl;

    @Override
    public String storeFile(String fileName, byte[] fileData) throws IOException {
        // Create upload directory if it doesn't exist
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        // Clean the filename
        String cleanFileName = StringUtils.cleanPath(fileName);
        if (cleanFileName.contains("..")) {
            throw new IOException("Invalid file name: " + cleanFileName);
        }

        // Store the file
        Path targetLocation = uploadPath.resolve(cleanFileName);
        Files.write(targetLocation, fileData);

        // Return the URL to access the file
        return baseUrl + "/" + cleanFileName;
    }

    @Override
    public void deleteFile(String fileName) throws IOException {
        Path filePath = Paths.get(uploadDir).resolve(fileName);
        Files.deleteIfExists(filePath);
    }

    @Override
    public String generateReceiptFileName(Long expenseId, String originalExtension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Ensure extension has a dot
        if (originalExtension != null && !originalExtension.startsWith(".")) {
            originalExtension = "." + originalExtension;
        } else if (originalExtension == null) {
            originalExtension = ".pdf"; // default extension
        }

        return String.format("receipt_expense_%d_%s_%s%s", expenseId, timestamp, uniqueId, originalExtension);
    }
}

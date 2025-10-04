package com.expenses.expensetracker.service;

import java.io.IOException;

public interface FileStorageService {
    /**
     * Stores a file and returns the URL to access it
     * @param fileName The name of the file
     * @param fileData The file content as byte array
     * @return The URL to access the stored file
     * @throws IOException if file storage fails
     */
    String storeFile(String fileName, byte[] fileData) throws IOException;

    /**
     * Deletes a file from storage
     * @param fileName The name of the file to delete
     * @throws IOException if file deletion fails
     */
    void deleteFile(String fileName) throws IOException;

    /**
     * Generates a unique filename for a receipt
     * @param expenseId The expense ID
     * @param originalExtension The original file extension (e.g., "pdf", "jpg")
     * @return A unique filename
     */
    String generateReceiptFileName(Long expenseId, String originalExtension);
}

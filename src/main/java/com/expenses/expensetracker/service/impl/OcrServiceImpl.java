package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.dto.ExpenseOcrResult;
import com.expenses.expensetracker.service.OcrService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.time.LocalDate;
import java.math.BigDecimal;

@Service
public class OcrServiceImpl implements OcrService {

    @Value("${expense.ocr.api.url}")
    private String ocrApiUrl;

    @Value("${expense.ocr.api.key}")
    private String ocrApiKey;

    private final RestTemplate restTemplate;

    public OcrServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public ExpenseOcrResult processReceipt(byte[] receiptImage) {
        try {
            // Prepare headers with API key
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", ocrApiKey);

            // Prepare request body
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("image", java.util.Base64.getEncoder().encodeToString(receiptImage));
            requestBody.put("language", "eng");
            requestBody.put("detectTables", true);

            // Make API call
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.exchange(
                ocrApiUrl,
                HttpMethod.POST,
                request,
                Map.class
            );

            // Process response
            return processOcrResponse(response.getBody());
        } catch (Exception e) {
            ExpenseOcrResult result = new ExpenseOcrResult();
            result.setSuccess(false);
            result.setErrorMessage("OCR processing failed: " + e.getMessage());
            return result;
        }
    }

    private ExpenseOcrResult processOcrResponse(Map<String, Object> ocrData) {
        ExpenseOcrResult result = new ExpenseOcrResult();
        try {
            // Extract data using intelligent parsing
            Map<String, Object> extractedData = extractRelevantData(ocrData);

            // Set extracted values
            result.setAmount(new BigDecimal(extractedData.get("total").toString()));
            result.setDate(LocalDate.parse(extractedData.get("date").toString()));
            result.setDescription(extractedData.get("description").toString());
            result.setMerchantName(extractedData.get("merchant").toString());
            result.setExpenseType(determineCategory(extractedData));
            result.setExpenseLines((String[]) extractedData.get("lineItems"));
            result.setSuccess(true);
        } catch (Exception e) {
            result.setSuccess(false);
            result.setErrorMessage("Failed to parse OCR data: " + e.getMessage());
        }
        return result;
    }

    private Map<String, Object> extractRelevantData(Map<String, Object> ocrData) {
        Map<String, Object> extracted = new HashMap<>();

        // Extract total amount using regex patterns
        String text = ocrData.get("text").toString().toLowerCase();
        extracted.put("total", extractAmount(text));
        extracted.put("date", extractDate(text));
        extracted.put("merchant", extractMerchant(text));
        extracted.put("description", generateDescription(text));
        extracted.put("lineItems", extractLineItems(text));

        return extracted;
    }

    private BigDecimal extractAmount(String text) {
        // Implementation to extract amount using regex patterns
        return BigDecimal.ZERO; // Placeholder
    }

    private LocalDate extractDate(String text) {
        // Implementation to extract date using various date formats
        return LocalDate.now(); // Placeholder
    }

    private String extractMerchant(String text) {
        // Implementation to extract merchant name
        return "Unknown Merchant"; // Placeholder
    }

    private String generateDescription(String text) {
        // Implementation to generate meaningful description
        return "Expense from receipt"; // Placeholder
    }

    private String[] extractLineItems(String text) {
        return new String[]{"Item 1"}; // Placeholder
    }

    private String determineCategory(Map<String, Object> data) {
        String merchantName = data.get("merchant").toString().toLowerCase();

        if (containsAny(merchantName, "airline", "hotel", "train", "taxi")) {
            return "TRAVEL";
        } else if (containsAny(merchantName, "restaurant", "cafe", "food")) {
            return "MEALS";
        } else if (containsAny(merchantName, "office", "supplies", "stationary")) {
            return "SUPPLIES";
        }

        return "OTHER";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}

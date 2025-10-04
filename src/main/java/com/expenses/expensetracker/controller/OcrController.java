package com.expenses.expensetracker.controller;

import com.expenses.expensetracker.dto.ExpenseOcrResult;
import com.expenses.expensetracker.service.OcrService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
public class OcrController {

    @Autowired
    private OcrService ocrService;

    @PostMapping("/process")
    public ResponseEntity<ExpenseOcrResult> processReceipt(@RequestParam("receipt") MultipartFile receipt) {
        try {
            ExpenseOcrResult result = ocrService.processReceipt(receipt.getBytes());
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            ExpenseOcrResult error = new ExpenseOcrResult();
            error.setSuccess(false);
            error.setErrorMessage("Failed to process receipt: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}

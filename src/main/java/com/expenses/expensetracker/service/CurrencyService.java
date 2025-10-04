package com.expenses.expensetracker.service;

import java.math.BigDecimal;
import java.util.Map;

public interface CurrencyService {
    // Required for company setup
    String getCountryCurrency(String countryCode);

    // Required for expense conversion
    BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency);
    Map<String, String> getAvailableCurrencies();

    // Required for display
    BigDecimal getExchangeRate(String fromCurrency, String toCurrency);
}

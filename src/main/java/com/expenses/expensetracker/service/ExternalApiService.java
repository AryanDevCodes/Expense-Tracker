package com.expenses.expensetracker.service;

import java.math.BigDecimal;
import java.util.Map;

public interface ExternalApiService {

    Map<String, String> getAvailableCurrencies();

    Map<String, Object> getCountryInfo(String countryCode);


    Map<String, BigDecimal> getExchangeRates(String baseCurrency);

    BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency);


    String getCountryCurrency(String countryCode);
}

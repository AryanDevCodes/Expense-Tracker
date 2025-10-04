package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.service.ExternalApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.cache.annotation.Cacheable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class ExternalApiServiceImpl implements ExternalApiService {

    @Value("${expense.api.countries.url}")
    private String countriesApiUrl;

    @Value("${expense.api.exchange.url}")
    private String exchangeRateApiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Cacheable("countryCurrencies")
    public Map<String, String> getAvailableCurrencies() {
        Map<String, Object>[] response = restTemplate.getForObject(
            countriesApiUrl + "?fields=name,currencies",
            Map[].class
        );

        Map<String, String> result = new HashMap<>();
        for (Map<String, Object> country : response) {
            @SuppressWarnings("unchecked")
            Map<String, Object> currencies = (Map<String, Object>) country.get("currencies");
            if (currencies != null) {
                currencies.forEach((code, details) -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> currencyInfo = (Map<String, String>) details;
                    result.put(code, currencyInfo.get("name"));
                });
            }
        }

        return result;
    }

    @Override
    @Cacheable("countryInfo")
    public Map<String, Object> getCountryInfo(String countryCode) {
        Map<String, Object>[] response = restTemplate.getForObject(
            countriesApiUrl + "?codes=" + countryCode,
            Map[].class
        );

        if (response != null && response.length > 0) {
            return response[0];
        }
        throw new IllegalArgumentException("Country not found: " + countryCode);
    }

    @Override
    @Cacheable(value = "exchangeRates", key = "#baseCurrency")
    public Map<String, BigDecimal> getExchangeRates(String baseCurrency) {
        String url = exchangeRateApiUrl + baseCurrency;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        Map<String, BigDecimal> rates = new HashMap<>();
        if (response != null && response.has("rates")) {
            JsonNode ratesNode = response.get("rates");
            ratesNode.fields().forEachRemaining(entry -> {
                rates.put(entry.getKey(), new BigDecimal(entry.getValue().asText()));
            });
        }

        return rates;
    }

    @Override
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        Map<String, BigDecimal> rates = getExchangeRates(fromCurrency);
        BigDecimal rate = rates.get(toCurrency);
        if (rate == null) {
            throw new IllegalArgumentException("No exchange rate found for " + toCurrency);
        }

        return amount.multiply(rate);
    }

    @Override
    public String getCountryCurrency(String countryCode) {
        Map<String, Object> countryInfo = getCountryInfo(countryCode);
        @SuppressWarnings("unchecked")
        Map<String, Object> currencies = (Map<String, Object>) countryInfo.get("currencies");
        if (currencies != null && !currencies.isEmpty()) {
            return currencies.keySet().iterator().next();
        }
        throw new IllegalArgumentException("No currency found for country: " + countryCode);
    }
}

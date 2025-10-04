package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.service.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Service
public class CurrencyServiceImpl implements CurrencyService {

    @Value("${expense.api.countries.url}")
    private String countriesApiUrl;

    @Value("${expense.api.exchange.url}")
    private String exchangeRateApiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Override
    @Cacheable("countryCurrencies")
    public String getCountryCurrency(String countryCode) {
        String url = countriesApiUrl + "?fields=name,currencies&codes=" + countryCode;
        Map<String, Object>[] response = restTemplate.getForObject(url, Map[].class);

        if (response == null || response.length == 0) {
            throw new IllegalArgumentException("Country not found: " + countryCode);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> currencies = (Map<String, Object>) response[0].get("currencies");
        return currencies.keySet().iterator().next();
    }

    @Override
    public BigDecimal convertAmount(BigDecimal amount, String fromCurrency, String toCurrency) {
        if (fromCurrency.equals(toCurrency)) {
            return amount;
        }

        BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    @Cacheable("currencies")
    public Map<String, String> getAvailableCurrencies() {
        Map<String, Object>[] response = restTemplate.getForObject(
            countriesApiUrl + "?fields=currencies",
            Map[].class
        );

        return extractCurrencies(response);
    }

    @Override
    @Cacheable("exchangeRates")
    public BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        String url = String.format(exchangeRateApiUrl, fromCurrency);
        Map<String, Object> response = restTemplate.getForObject(url, Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> rates = (Map<String, Object>) response.get("rates");
        return new BigDecimal(rates.get(toCurrency).toString());
    }

    private Map<String, String> extractCurrencies(Map<String, Object>[] countries) {
        return java.util.Arrays.stream(countries)
            .filter(country -> country.containsKey("currencies"))
            .flatMap(country -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> currencies = (Map<String, Object>) country.get("currencies");
                return currencies.entrySet().stream();
            })
            .collect(java.util.stream.Collectors.toMap(
                Map.Entry::getKey,
                e -> {
                    @SuppressWarnings("unchecked")
                    Map<String, String> details = (Map<String, String>) e.getValue();
                    return details.get("name");
                },
                (a, b) -> a // Keep first occurrence in case of duplicates
            ));
    }
}

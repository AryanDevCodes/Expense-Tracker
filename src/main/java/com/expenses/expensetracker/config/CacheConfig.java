package com.expenses.expensetracker.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager();
        // Define cache names for different data types
        cacheManager.setCacheNames(java.util.Arrays.asList(
            "userExpenses",
            "teamExpenses",
            "recentExpenses",
            "expenseCount",
            "pendingApprovals",
            "userRoles",
            "currencies"
        ));
        return cacheManager;
    }
}

package com.expenses.expensetracker.service;

import com.expenses.expensetracker.entity.Company;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface CompanyService {
    // Required for first login/signup
    Company createCompanyWithCurrency(String name, String country);

    // Required for admin
    void updateCompanyCurrency(Long companyId, String newCurrency);
    void setAutoApprovalThreshold(Long companyId, BigDecimal threshold);
    void setCfoApprovalThreshold(Long companyId, BigDecimal threshold);

    // Required for currency conversion
    String getCompanyCurrency(Long companyId);
    Optional<Company> findById(Long companyId);
    long count(); // Used to check if first company

    List<Company> findAll();
    Company createCompany(Company company);
    Company updateCompany(Company company);
    void deleteCompany(Long companyId);
}

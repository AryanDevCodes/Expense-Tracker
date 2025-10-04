package com.expenses.expensetracker.service.impl;

import com.expenses.expensetracker.entity.Company;
import com.expenses.expensetracker.repository.CompanyRepository;
import com.expenses.expensetracker.service.CompanyService;
import com.expenses.expensetracker.service.ExternalApiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CompanyServiceImpl implements CompanyService {

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ExternalApiService externalApiService;

    @Override
    public Company createCompanyWithCurrency(String name, String country) {
        // Get country's currency from external API
        String currency = externalApiService.getCountryCurrency(country);
        if (currency == null) {
            throw new IllegalArgumentException("Could not determine currency for country: " + country);
        }

        Company company = new Company();
        company.setName(name);
        company.setCountry(country);
        company.setDefaultCurrency(currency);
        company.setRequireManagerApproval(true); // Default setting
        company.setAllowMultiCurrency(true);     // Default setting

        return companyRepository.save(company);
    }

    @Override
    public void updateCompanyCurrency(Long companyId, String newCurrency) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        company.setDefaultCurrency(newCurrency);
        companyRepository.save(company);
    }

    @Override
    public void setAutoApprovalThreshold(Long companyId, BigDecimal threshold) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        company.setAutoApprovalThreshold(threshold);
        companyRepository.save(company);
    }

    @Override
    public void setCfoApprovalThreshold(Long companyId, BigDecimal threshold) {
        Company company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
        company.setCfoApprovalThreshold(threshold);
        companyRepository.save(company);
    }

    @Override
    public String getCompanyCurrency(Long companyId) {
        return companyRepository.findById(companyId)
            .map(Company::getDefaultCurrency)
            .orElseThrow(() -> new IllegalArgumentException("Company not found"));
    }

    @Override
    public List<Company> findAll() {
        return companyRepository.findAll();
    }

    @Override
    public Company createCompany(Company company) {
        return companyRepository.save(company);
    }

    @Override
    public Company updateCompany(Company company) {
        return companyRepository.save(company);
    }

    @Override
    public void deleteCompany(Long companyId) {
        companyRepository.deleteById(companyId);
    }

    @Override
    public Optional<Company> findById(Long companyId) {
        return companyRepository.findById(companyId);
    }

    @Override
    public long count() {
        return companyRepository.count();
    }
}

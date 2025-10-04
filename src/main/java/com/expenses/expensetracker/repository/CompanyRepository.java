package com.expenses.expensetracker.repository;

import com.expenses.expensetracker.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<Company, Long> {
}

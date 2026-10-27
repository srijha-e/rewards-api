package com.retailer.rewards.repository;

import java.time.LocalDate;
import java.util.List;

import com.retailer.rewards.model.Transaction;

public interface TransactionRepository {

    List<Transaction> findAll();

    List<Transaction> findByCustomerId(long customerId);

    /** Returns transactions dated between {@code startDate} and {@code endDate}, both inclusive. */
    List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate);

    /** Returns one customer's transactions dated between {@code startDate} and {@code endDate}, both inclusive. */
    List<Transaction> findByCustomerIdAndDateRange(long customerId, LocalDate startDate, LocalDate endDate);
}

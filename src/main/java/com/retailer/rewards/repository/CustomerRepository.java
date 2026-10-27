package com.retailer.rewards.repository;

import java.util.List;
import java.util.Optional;

import com.retailer.rewards.model.Customer;

public interface CustomerRepository {

    List<Customer> findAll();

    Optional<Customer> findById(long customerId);
}

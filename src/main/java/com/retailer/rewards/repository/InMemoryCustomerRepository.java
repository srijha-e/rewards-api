package com.retailer.rewards.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.retailer.rewards.model.Customer;

/**
 * Sample customer data. Swap this for a JPA/JDBC implementation to use a real database.
 */
@Repository
public class InMemoryCustomerRepository implements CustomerRepository {

    private final List<Customer> customers = List.of(
            new Customer(1L, "Alice Johnson"),
            new Customer(2L, "Bob Smith"),
            new Customer(3L, "Carol Davis"),
            new Customer(4L, "David Lee"));

    @Override
    public List<Customer> findAll() {
        return customers;
    }

    @Override
    public Optional<Customer> findById(long customerId) {
        return customers.stream()
                .filter(customer -> customer.customerId() == customerId)
                .findFirst();
    }
}

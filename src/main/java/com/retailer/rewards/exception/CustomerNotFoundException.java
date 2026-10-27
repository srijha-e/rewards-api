package com.retailer.rewards.exception;

public class CustomerNotFoundException extends RuntimeException {

    public CustomerNotFoundException(long customerId) {
        super("Customer not found: " + customerId);
    }
}

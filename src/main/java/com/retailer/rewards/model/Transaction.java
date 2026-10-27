package com.retailer.rewards.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single purchase. {@code amount} is in dollars; BigDecimal keeps cents exact.
 */
public record Transaction(long transactionId, long customerId, BigDecimal amount, LocalDate transactionDate) {
}

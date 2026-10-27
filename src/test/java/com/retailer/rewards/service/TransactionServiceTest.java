package com.retailer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.InMemoryCustomerRepository;
import com.retailer.rewards.repository.InMemoryTransactionRepository;
import com.retailer.rewards.repository.TransactionRepository;

/**
 * Uses the sample data with "today" fixed at 2026-10-15.
 */
class TransactionServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(LocalDate.of(2026, 10, 15).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final TransactionService transactionService = new TransactionService(
            new InMemoryCustomerRepository(),
            new InMemoryTransactionRepository(FIXED_CLOCK));

    @Test
    void returnsEveryTransactionIncludingThoseOutsideTheRewardsWindow() {
        List<Transaction> allTransactions = transactionService.getAllTransactions();

        assertThat(allTransactions).hasSize(13);
        assertThat(allTransactions).extracting(Transaction::transactionDate)
                .contains(LocalDate.of(2026, 7, 20), LocalDate.of(2026, 7, 10));
    }

    @Test
    void returnsOnlyTheRequestedCustomersTransactions() {
        List<Transaction> carolTransactions = transactionService.getTransactionsForCustomer(3L);

        assertThat(carolTransactions).extracting(Transaction::customerId).containsOnly(3L);
        assertThat(carolTransactions).extracting(Transaction::amount)
                .containsExactly(new BigDecimal("250.75"), new BigDecimal("130.00"));
    }

    @Test
    void returnsEmptyListForCustomerWithNoTransactions() {
        TransactionService serviceWithNoTransactions = new TransactionService(
                new InMemoryCustomerRepository(),
                new EmptyTransactionRepository());

        assertThat(serviceWithNoTransactions.getTransactionsForCustomer(1L)).isEmpty();
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        assertThatThrownBy(() -> transactionService.getTransactionsForCustomer(99L))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found: 99");
    }

    /** A data source with no transactions, to check the service handles it. */
    private static final class EmptyTransactionRepository implements TransactionRepository {

        @Override
        public List<Transaction> findAll() {
            return List.of();
        }

        @Override
        public List<Transaction> findByCustomerId(long customerId) {
            return List.of();
        }

        @Override
        public List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) {
            return List.of();
        }

        @Override
        public List<Transaction> findByCustomerIdAndDateRange(long customerId, LocalDate startDate, LocalDate endDate) {
            return List.of();
        }
    }
}

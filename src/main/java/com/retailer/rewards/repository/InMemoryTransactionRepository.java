package com.retailer.rewards.repository;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.retailer.rewards.model.Transaction;

/**
 * Sample transaction data, dated relative to the current month so the default
 * three-month window always has data to show. It deliberately includes one
 * transaction per customer that is older than three months, plus the boundary
 * amounts $50, $51, $100, $100.99 and $101.
 */
@Repository
public class InMemoryTransactionRepository implements TransactionRepository {

    private final List<Transaction> transactions;

    public InMemoryTransactionRepository(Clock clock) {
        this.transactions = buildSampleTransactions(YearMonth.now(clock));
    }

    @Override
    public List<Transaction> findAll() {
        return transactions;
    }

    @Override
    public List<Transaction> findByCustomerId(long customerId) {
        return transactions.stream()
                .filter(transaction -> transaction.customerId() == customerId)
                .toList();
    }

    @Override
    public List<Transaction> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return transactions.stream()
                .filter(transaction -> isWithin(transaction, startDate, endDate))
                .toList();
    }

    @Override
    public List<Transaction> findByCustomerIdAndDateRange(long customerId, LocalDate startDate, LocalDate endDate) {
        return transactions.stream()
                .filter(transaction -> transaction.customerId() == customerId)
                .filter(transaction -> isWithin(transaction, startDate, endDate))
                .toList();
    }

    private static boolean isWithin(Transaction transaction, LocalDate startDate, LocalDate endDate) {
        LocalDate transactionDate = transaction.transactionDate();
        return !transactionDate.isBefore(startDate) && !transactionDate.isAfter(endDate);
    }

    private static List<Transaction> buildSampleTransactions(YearMonth currentMonth) {
        YearMonth oneMonthAgo = currentMonth.minusMonths(1);
        YearMonth twoMonthsAgo = currentMonth.minusMonths(2);
        YearMonth threeMonthsAgo = currentMonth.minusMonths(3);

        // Current-month purchases are dated on the 1st so they are never in the future.
        return List.of(
                // Alice: the $120 example from the requirements, plus one purchase outside the window
                new Transaction(1L, 1L, new BigDecimal("120.00"), twoMonthsAgo.atDay(5)),
                new Transaction(2L, 1L, new BigDecimal("75.50"), twoMonthsAgo.atDay(18)),
                new Transaction(3L, 1L, new BigDecimal("200.00"), oneMonthAgo.atDay(10)),
                new Transaction(4L, 1L, new BigDecimal("45.00"), currentMonth.atDay(1)),
                new Transaction(5L, 1L, new BigDecimal("500.00"), threeMonthsAgo.atDay(20)),

                // Bob: boundary amounts
                new Transaction(6L, 2L, new BigDecimal("100.00"), twoMonthsAgo.atDay(3)),
                new Transaction(7L, 2L, new BigDecimal("50.00"), twoMonthsAgo.atDay(25)),
                new Transaction(8L, 2L, new BigDecimal("51.00"), oneMonthAgo.atDay(7)),
                new Transaction(9L, 2L, new BigDecimal("100.99"), oneMonthAgo.atDay(21)),
                new Transaction(10L, 2L, new BigDecimal("101.00"), currentMonth.atDay(1)),

                // Carol: no purchases last month, so that month shows 0 points
                new Transaction(11L, 3L, new BigDecimal("250.75"), twoMonthsAgo.atDay(12)),
                new Transaction(12L, 3L, new BigDecimal("130.00"), currentMonth.atDay(1)),

                // David: only an old purchase, so he earns 0 points in the window
                new Transaction(13L, 4L, new BigDecimal("90.00"), threeMonthsAgo.atDay(10)));
    }
}

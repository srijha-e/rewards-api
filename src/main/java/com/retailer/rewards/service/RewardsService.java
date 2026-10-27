package com.retailer.rewards.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyRewardPoints;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.model.Customer;
import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.repository.CustomerRepository;
import com.retailer.rewards.repository.TransactionRepository;

/**
 * Works out each customer's reward points per month and in total for a date range.
 * The points for a single transaction come from {@link RewardPointsCalculator}.
 */
@Service
public class RewardsService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final RewardPointsCalculator rewardPointsCalculator;
    private final Clock clock;
    private final int defaultPeriodMonths;

    public RewardsService(CustomerRepository customerRepository,
                          TransactionRepository transactionRepository,
                          RewardPointsCalculator rewardPointsCalculator,
                          Clock clock,
                          @Value("${rewards.default-period-months:3}") int defaultPeriodMonths) {
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.rewardPointsCalculator = rewardPointsCalculator;
        this.clock = clock;
        this.defaultPeriodMonths = defaultPeriodMonths;
    }

    /**
     * Rewards for every customer. Dates are optional: endDate defaults to today and
     * startDate to the first day of the month {@code defaultPeriodMonths - 1} months before endDate.
     */
    public List<CustomerRewardsResponse> getRewardsForAllCustomers(LocalDate startDate, LocalDate endDate) {
        LocalDate resolvedEndDate = resolveEndDate(endDate);
        LocalDate resolvedStartDate = resolveStartDate(startDate, resolvedEndDate);

        Map<Long, List<Transaction>> transactionsByCustomerId = transactionRepository
                .findByDateRange(resolvedStartDate, resolvedEndDate)
                .stream()
                .collect(Collectors.groupingBy(Transaction::customerId));

        return customerRepository.findAll().stream()
                .map(customer -> buildCustomerRewards(customer,
                        transactionsByCustomerId.getOrDefault(customer.customerId(), List.of()),
                        resolvedStartDate, resolvedEndDate))
                .toList();
    }

    public CustomerRewardsResponse getRewardsForCustomer(long customerId, LocalDate startDate, LocalDate endDate) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        LocalDate resolvedEndDate = resolveEndDate(endDate);
        LocalDate resolvedStartDate = resolveStartDate(startDate, resolvedEndDate);

        List<Transaction> customerTransactions = transactionRepository
                .findByCustomerIdAndDateRange(customerId, resolvedStartDate, resolvedEndDate);

        return buildCustomerRewards(customer, customerTransactions, resolvedStartDate, resolvedEndDate);
    }

    private LocalDate resolveEndDate(LocalDate endDate) {
        return endDate != null ? endDate : LocalDate.now(clock);
    }

    private LocalDate resolveStartDate(LocalDate startDate, LocalDate resolvedEndDate) {
        LocalDate resolvedStartDate = startDate != null
                ? startDate
                : YearMonth.from(resolvedEndDate).minusMonths(defaultPeriodMonths - 1L).atDay(1);

        if (resolvedStartDate.isAfter(resolvedEndDate)) {
            throw new InvalidDateRangeException(resolvedStartDate, resolvedEndDate);
        }
        return resolvedStartDate;
    }

    private CustomerRewardsResponse buildCustomerRewards(Customer customer,
                                                         List<Transaction> customerTransactions,
                                                         LocalDate startDate,
                                                         LocalDate endDate) {
        Map<YearMonth, Long> pointsByMonth = customerTransactions.stream()
                .collect(Collectors.groupingBy(
                        transaction -> YearMonth.from(transaction.transactionDate()),
                        Collectors.summingLong(transaction -> rewardPointsCalculator.calculatePoints(transaction.amount()))));

        // Every month in the range is listed, including months with 0 points, oldest first.
        List<MonthlyRewardPoints> monthlyRewards = new ArrayList<>();
        for (YearMonth month = YearMonth.from(startDate); !month.isAfter(YearMonth.from(endDate)); month = month.plusMonths(1)) {
            monthlyRewards.add(new MonthlyRewardPoints(month, pointsByMonth.getOrDefault(month, 0L)));
        }

        long totalPoints = monthlyRewards.stream().mapToLong(MonthlyRewardPoints::points).sum();

        return new CustomerRewardsResponse(customer.customerId(), customer.customerName(),
                startDate, endDate, monthlyRewards, totalPoints);
    }
}

package com.retailer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.dto.MonthlyRewardPoints;
import com.retailer.rewards.exception.CustomerNotFoundException;
import com.retailer.rewards.exception.InvalidDateRangeException;
import com.retailer.rewards.repository.InMemoryCustomerRepository;
import com.retailer.rewards.repository.InMemoryTransactionRepository;

/**
 * Uses the sample data with "today" fixed at 2026-10-15, so the default window is
 * 2026-08-01 to 2026-10-15 and the out-of-window purchases fall in July 2026.
 */
class RewardsServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);
    private static final Clock FIXED_CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    private final RewardsService rewardsService = new RewardsService(
            new InMemoryCustomerRepository(),
            new InMemoryTransactionRepository(FIXED_CLOCK),
            new RewardPointsCalculator(),
            FIXED_CLOCK,
            3);

    @Test
    void defaultWindowCoversCurrentMonthAndTwoPreviousMonths() {
        CustomerRewardsResponse aliceRewards = rewardsService.getRewardsForCustomer(1L, null, null);

        assertThat(aliceRewards.startDate()).isEqualTo(LocalDate.of(2026, 8, 1));
        assertThat(aliceRewards.endDate()).isEqualTo(TODAY);
    }

    @Test
    void calculatesMonthlyAndTotalPointsAndExcludesOlderTransactions() {
        CustomerRewardsResponse aliceRewards = rewardsService.getRewardsForCustomer(1L, null, null);

        // Aug: $120 -> 90 and $75.50 -> 25; Sep: $200 -> 250; Oct: $45 -> 0. July's $500 is excluded.
        assertThat(aliceRewards.monthlyRewards()).containsExactly(
                new MonthlyRewardPoints(YearMonth.of(2026, 8), 115),
                new MonthlyRewardPoints(YearMonth.of(2026, 9), 250),
                new MonthlyRewardPoints(YearMonth.of(2026, 10), 0));
        assertThat(aliceRewards.totalPoints()).isEqualTo(365);
    }

    @Test
    void handlesBoundaryAmounts() {
        CustomerRewardsResponse bobRewards = rewardsService.getRewardsForCustomer(2L, null, null);

        // Aug: $100 -> 50 and $50 -> 0; Sep: $51 -> 1 and $100.99 -> 50; Oct: $101 -> 52.
        assertThat(bobRewards.monthlyRewards()).extracting(MonthlyRewardPoints::points)
                .containsExactly(50L, 51L, 52L);
        assertThat(bobRewards.totalPoints()).isEqualTo(153);
    }

    @Test
    void listsMonthsWithoutPurchasesAsZeroPoints() {
        CustomerRewardsResponse carolRewards = rewardsService.getRewardsForCustomer(3L, null, null);

        assertThat(carolRewards.monthlyRewards()).extracting(MonthlyRewardPoints::points)
                .containsExactly(350L, 0L, 110L);
        assertThat(carolRewards.totalPoints()).isEqualTo(460);
    }

    @Test
    void returnsEveryCustomerIncludingThoseWithNoPointsInWindow() {
        List<CustomerRewardsResponse> allRewards = rewardsService.getRewardsForAllCustomers(null, null);

        assertThat(allRewards).extracting(CustomerRewardsResponse::customerId).containsExactly(1L, 2L, 3L, 4L);
        assertThat(allRewards).extracting(CustomerRewardsResponse::totalPoints).containsExactly(365L, 153L, 460L, 0L);
    }

    @Test
    void usesRequestedDateRangeWhenProvided() {
        LocalDate julyStart = LocalDate.of(2026, 7, 1);
        LocalDate julyEnd = LocalDate.of(2026, 7, 31);

        CustomerRewardsResponse aliceRewards = rewardsService.getRewardsForCustomer(1L, julyStart, julyEnd);

        assertThat(aliceRewards.monthlyRewards())
                .containsExactly(new MonthlyRewardPoints(YearMonth.of(2026, 7), 850));
        assertThat(aliceRewards.totalPoints()).isEqualTo(850);
    }

    @Test
    void usesTodayAsEndDateWhenOnlyStartDateIsProvided() {
        CustomerRewardsResponse aliceRewards = rewardsService.getRewardsForCustomer(1L, LocalDate.of(2026, 9, 1), null);

        assertThat(aliceRewards.endDate()).isEqualTo(TODAY);
        assertThat(aliceRewards.monthlyRewards()).containsExactly(
                new MonthlyRewardPoints(YearMonth.of(2026, 9), 250),
                new MonthlyRewardPoints(YearMonth.of(2026, 10), 0));
        assertThat(aliceRewards.totalPoints()).isEqualTo(250);
    }

    @Test
    void startsThreeMonthsBackWhenOnlyEndDateIsProvided() {
        CustomerRewardsResponse aliceRewards = rewardsService.getRewardsForCustomer(1L, null, LocalDate.of(2026, 9, 30));

        // Jul: $500 -> 850; Aug: 115; Sep: 250
        assertThat(aliceRewards.startDate()).isEqualTo(LocalDate.of(2026, 7, 1));
        assertThat(aliceRewards.monthlyRewards()).extracting(MonthlyRewardPoints::points)
                .containsExactly(850L, 115L, 250L);
        assertThat(aliceRewards.totalPoints()).isEqualTo(1215);
    }

    @Test
    void groupsMonthsCorrectlyAcrossYearEnd() {
        Clock januaryClock = Clock.fixed(LocalDate.of(2027, 1, 15).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        RewardsService januaryRewardsService = new RewardsService(
                new InMemoryCustomerRepository(),
                new InMemoryTransactionRepository(januaryClock),
                new RewardPointsCalculator(),
                januaryClock,
                3);

        CustomerRewardsResponse aliceRewards = januaryRewardsService.getRewardsForCustomer(1L, null, null);

        assertThat(aliceRewards.monthlyRewards()).containsExactly(
                new MonthlyRewardPoints(YearMonth.of(2026, 11), 115),
                new MonthlyRewardPoints(YearMonth.of(2026, 12), 250),
                new MonthlyRewardPoints(YearMonth.of(2027, 1), 0));
        assertThat(aliceRewards.totalPoints()).isEqualTo(365);
    }

    @Test
    void appliesRequestedDateRangeToAllCustomers() {
        List<CustomerRewardsResponse> julyRewards =
                rewardsService.getRewardsForAllCustomers(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31));

        // Only Alice ($500 -> 850) and David ($90 -> 40) bought anything in July
        assertThat(julyRewards).extracting(CustomerRewardsResponse::totalPoints).containsExactly(850L, 0L, 0L, 40L);
    }

    @Test
    void rejectsStartDateAfterEndDate() {
        LocalDate startDate = LocalDate.of(2026, 9, 1);
        LocalDate endDate = LocalDate.of(2026, 8, 1);

        assertThatThrownBy(() -> rewardsService.getRewardsForCustomer(1L, startDate, endDate))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    @Test
    void throwsWhenCustomerDoesNotExist() {
        assertThatThrownBy(() -> rewardsService.getRewardsForCustomer(99L, null, null))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessageContaining("99");
    }
}

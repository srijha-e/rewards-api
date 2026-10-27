package com.retailer.rewards.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RewardPointsCalculatorTest {

    private final RewardPointsCalculator rewardPointsCalculator = new RewardPointsCalculator();

    @ParameterizedTest(name = "${0} earns {1} points")
    @CsvSource({
            "0.00,     0",
            "49.99,    0",
            "50.00,    0",
            "50.99,    0",
            "51.00,    1",
            "75.50,   25",
            "99.99,   49",
            "100.00,  50",
            "100.99,  50",
            "101.00,  52",
            "120.00,  90",
            "120.75,  90",
            "200.00, 250",
            "250.75, 350"
    })
    void calculatesPointsForTransactionAmount(String transactionAmount, long expectedPoints) {
        assertThat(rewardPointsCalculator.calculatePoints(new BigDecimal(transactionAmount)))
                .isEqualTo(expectedPoints);
    }

    @Test
    void rejectsNegativeAmount() {
        assertThatThrownBy(() -> rewardPointsCalculator.calculatePoints(new BigDecimal("-1.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullAmount() {
        assertThatThrownBy(() -> rewardPointsCalculator.calculatePoints(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package com.retailer.rewards.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

/**
 * Points for a single transaction:
 * 2 points per whole dollar over $100, plus 1 point per whole dollar between $50 and $100.
 * Cents are ignored, so $120.75 earns the same as $120 (90 points).
 */
@Component
public class RewardPointsCalculator {

    private static final long LOWER_THRESHOLD_DOLLARS = 50;
    private static final long UPPER_THRESHOLD_DOLLARS = 100;
    private static final long POINTS_PER_DOLLAR_OVER_UPPER = 2;

    public long calculatePoints(BigDecimal transactionAmount) {
        if (transactionAmount == null || transactionAmount.signum() < 0) {
            throw new IllegalArgumentException("Transaction amount must be zero or positive: " + transactionAmount);
        }

        long wholeDollars = transactionAmount.setScale(0, RoundingMode.DOWN).longValueExact();

        long dollarsOverUpper = Math.max(0, wholeDollars - UPPER_THRESHOLD_DOLLARS);
        long dollarsBetweenThresholds = Math.max(0,
                Math.min(wholeDollars, UPPER_THRESHOLD_DOLLARS) - LOWER_THRESHOLD_DOLLARS);

        return dollarsOverUpper * POINTS_PER_DOLLAR_OVER_UPPER + dollarsBetweenThresholds;
    }
}

package com.retailer.rewards.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record CustomerRewardsResponse(
        @Schema(example = "1") long customerId,
        @Schema(example = "Alice Johnson") String customerName,
        @Schema(description = "Start of the period, inclusive", example = "2026-08-01") LocalDate startDate,
        @Schema(description = "End of the period, inclusive", example = "2026-10-31") LocalDate endDate,
        @Schema(description = "Every month in the period, oldest first, including months with 0 points")
        List<MonthlyRewardPoints> monthlyRewards,
        @Schema(example = "365") long totalPoints) {
}

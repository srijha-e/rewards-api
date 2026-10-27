package com.retailer.rewards.dto;

import java.time.YearMonth;

import io.swagger.v3.oas.annotations.media.Schema;

/** Serialized as {"month": "2026-08", "points": 90}. */
public record MonthlyRewardPoints(
        @Schema(type = "string", format = "yyyy-MM", example = "2026-08") YearMonth month,
        @Schema(example = "90") long points) {
}

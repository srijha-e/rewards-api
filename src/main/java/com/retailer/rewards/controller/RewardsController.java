package com.retailer.rewards.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.retailer.rewards.dto.CustomerRewardsResponse;
import com.retailer.rewards.service.RewardsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/rewards")
@Tag(name = "Rewards", description = "Reward points per customer, per month and in total")
public class RewardsController {

    private static final String START_DATE_DESCRIPTION =
            "Inclusive start date (yyyy-MM-dd). Defaults to the first day of the month two months before endDate.";
    private static final String END_DATE_DESCRIPTION =
            "Inclusive end date (yyyy-MM-dd). Defaults to today.";

    private final RewardsService rewardsService;

    public RewardsController(RewardsService rewardsService) {
        this.rewardsService = rewardsService;
    }

    @GetMapping
    @Operation(summary = "Get rewards for all customers")
    @ApiResponse(responseCode = "200", description = "Rewards for every customer")
    @ApiResponse(responseCode = "400", description = "Invalid or out-of-order dates",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public List<CustomerRewardsResponse> getRewardsForAllCustomers(
            @Parameter(description = START_DATE_DESCRIPTION, example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = END_DATE_DESCRIPTION, example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return rewardsService.getRewardsForAllCustomers(startDate, endDate);
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get rewards for one customer")
    @ApiResponse(responseCode = "200", description = "Monthly and total rewards for the customer")
    @ApiResponse(responseCode = "400", description = "Invalid customer ID or dates",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerRewardsResponse getRewardsForCustomer(
            @Parameter(description = "Customer ID", example = "1")
            @PathVariable long customerId,
            @Parameter(description = START_DATE_DESCRIPTION, example = "2026-08-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = END_DATE_DESCRIPTION, example = "2026-10-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return rewardsService.getRewardsForCustomer(customerId, startDate, endDate);
    }
}

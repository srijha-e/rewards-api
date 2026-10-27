package com.retailer.rewards.controller;

import java.util.List;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.retailer.rewards.model.Transaction;
import com.retailer.rewards.service.TransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transactions", description = "The purchase transactions rewards are calculated from")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @GetMapping
    @Operation(summary = "Get transactions, optionally for one customer")
    @ApiResponse(responseCode = "200", description = "Matching transactions")
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public List<Transaction> getTransactions(
            @Parameter(description = "Return only this customer's transactions", example = "1")
            @RequestParam(required = false) Long customerId) {
        return customerId == null
                ? transactionService.getAllTransactions()
                : transactionService.getTransactionsForCustomer(customerId);
    }
}

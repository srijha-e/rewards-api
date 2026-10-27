package com.retailer.rewards.exception;

import java.time.LocalDate;

public class InvalidDateRangeException extends RuntimeException {

    public InvalidDateRangeException(LocalDate startDate, LocalDate endDate) {
        super("startDate " + startDate + " must not be after endDate " + endDate);
    }
}

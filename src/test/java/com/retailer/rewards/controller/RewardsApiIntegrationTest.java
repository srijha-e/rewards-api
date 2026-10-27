package com.retailer.rewards.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end tests through the HTTP layer with the full application context,
 * "today" fixed at 2026-10-15.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RewardsApiIntegrationTest {

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDate.of(2026, 10, 15).atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getRewardsReturnsAllCustomers() throws Exception {
        mockMvc.perform(get("/api/rewards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].customerName").value("Alice Johnson"))
                .andExpect(jsonPath("$[0].totalPoints").value(365))
                .andExpect(jsonPath("$[3].customerName").value("David Lee"))
                .andExpect(jsonPath("$[3].totalPoints").value(0));
    }

    @Test
    void getRewardsByCustomerIdReturnsMonthlyBreakdown() throws Exception {
        mockMvc.perform(get("/api/rewards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.startDate").value("2026-08-01"))
                .andExpect(jsonPath("$.endDate").value("2026-10-15"))
                .andExpect(jsonPath("$.monthlyRewards", hasSize(3)))
                .andExpect(jsonPath("$.monthlyRewards[0].month").value("2026-08"))
                .andExpect(jsonPath("$.monthlyRewards[0].points").value(115))
                .andExpect(jsonPath("$.monthlyRewards[1].month").value("2026-09"))
                .andExpect(jsonPath("$.monthlyRewards[1].points").value(250))
                .andExpect(jsonPath("$.monthlyRewards[2].month").value("2026-10"))
                .andExpect(jsonPath("$.monthlyRewards[2].points").value(0))
                .andExpect(jsonPath("$.totalPoints").value(365));
    }

    @Test
    void getRewardsByCustomerIdHonoursDateRange() throws Exception {
        mockMvc.perform(get("/api/rewards/4").param("startDate", "2026-07-01").param("endDate", "2026-07-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyRewards", hasSize(1)))
                .andExpect(jsonPath("$.totalPoints").value(40));
    }

    @Test
    void getRewardsForUnknownCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/rewards/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Customer not found: 99"));
    }

    @Test
    void getRewardsWithStartDateAfterEndDateReturns400() throws Exception {
        mockMvc.perform(get("/api/rewards").param("startDate", "2026-10-01").param("endDate", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getRewardsWithMalformedDateReturns400() throws Exception {
        mockMvc.perform(get("/api/rewards").param("startDate", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value 'not-a-date' for parameter 'startDate'"));
    }

    @Test
    void getRewardsWithNonNumericCustomerIdReturns400() throws Exception {
        mockMvc.perform(get("/api/rewards/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value 'abc' for parameter 'customerId'"));
    }

    @Test
    void getTransactionsReturnsAllTransactions() throws Exception {
        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(13)));
    }

    @Test
    void getTransactionsFiltersByCustomerId() throws Exception {
        mockMvc.perform(get("/api/transactions").param("customerId", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].amount").value(250.75))
                .andExpect(jsonPath("$[0].transactionDate").value("2026-08-12"));
    }

    @Test
    void getTransactionsForUnknownCustomerReturns404() throws Exception {
        mockMvc.perform(get("/api/transactions").param("customerId", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void openApiDocumentDescribesEveryEndpoint() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Rewards API"))
                .andExpect(jsonPath("$.paths['/api/rewards'].get").exists())
                .andExpect(jsonPath("$.paths['/api/rewards/{customerId}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/transactions'].get").exists());
    }
}

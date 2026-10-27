package com.retailer.rewards.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

/**
 * Title and description for the generated OpenAPI document.
 * Swagger UI: /swagger-ui.html, raw document: /v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI rewardsOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Rewards API")
                .version("1.0.0")
                .description("""
                        Calculates the reward points each customer earns per month and in total.

                        For each transaction: 2 points for every whole dollar over $100, \
                        plus 1 point for every whole dollar between $50 and $100. \
                        Example: $120 = 2 x 20 + 1 x 50 = 90 points.
                        """));
    }
}

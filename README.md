# Rewards API

A Spring Boot REST API that calculates the reward points each customer earns per month, and in total, over a three-month period.

## Reward rules

For each transaction:

- **2 points** for every whole dollar spent over $100
- **1 point** for every whole dollar spent between $50 and $100

Example: a $120 purchase earns 2 × 20 + 1 × 50 = **90 points**.

Cents are ignored, so $120.75 also earns 90 points. Some boundary cases:

| Amount  | Points |
|---------|--------|
| $50.00  | 0      |
| $51.00  | 1      |
| $100.00 | 50     |
| $100.99 | 50     |
| $101.00 | 52     |

## Tech stack

- Java 21 (records, text blocks, streams, `java.time`)
- Spring Boot 3.5 (Spring Web)
- springdoc-openapi for the OpenAPI document and Swagger UI (the only other runtime dependency)
- JUnit 5, AssertJ, MockMvc for tests; JaCoCo for the coverage report
- Maven Wrapper, so no local Maven install is needed

## Running the app

Requires JDK 21 or newer. No Maven install is needed.

```bash
git clone <repository-url>
cd rewards-api

# macOS / Linux
./mvnw spring-boot:run

# Windows
mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`. To use another port:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
```

## Running the tests

```bash
./mvnw test
```

This also writes a test coverage report (JaCoCo) to `target/site/jacoco/index.html`. Open it in a browser.

## API documentation (Swagger)

With the app running:

- Swagger UI: http://localhost:8080/swagger-ui.html (try every endpoint from the browser)
- OpenAPI document (JSON): http://localhost:8080/v3/api-docs
- OpenAPI document (YAML): http://localhost:8080/v3/api-docs.yaml

## Endpoints

| Method | Path                                    | Description                                         |
|--------|-----------------------------------------|-----------------------------------------------------|
| GET    | `/api/rewards`                          | Rewards for all customers                           |
| GET    | `/api/rewards/{customerId}`             | Rewards for one customer                            |
| GET    | `/api/transactions`                     | All transactions in the sample data                 |
| GET    | `/api/transactions?customerId={id}`     | Transactions for one customer                       |

### Date range

Both rewards endpoints accept the optional query parameters `startDate` and `endDate` (ISO format, `yyyy-MM-dd`, inclusive).

- `endDate` defaults to today.
- `startDate` defaults to the first day of the month two months before `endDate`, which covers the current month and the two months before it.

The period is applied when the transactions are queried, so the result is correct even if the data source holds older transactions. The window length can be changed with `rewards.default-period-months` in `application.properties`.

```bash
curl http://localhost:8080/api/rewards
curl http://localhost:8080/api/rewards/1
curl "http://localhost:8080/api/rewards/1?startDate=2026-07-01&endDate=2026-07-31"
curl http://localhost:8080/api/transactions?customerId=3
```

### Sample response: `GET /api/rewards/1`

Taken on 2026-10-24. The sample data is dated relative to the current month, so the months and dates you see will move with the date you run it; the points stay the same.

```json
{
  "customerId": 1,
  "customerName": "Alice Johnson",
  "startDate": "2026-08-01",
  "endDate": "2026-10-24",
  "monthlyRewards": [
    { "month": "2026-08", "points": 115 },
    { "month": "2026-09", "points": 250 },
    { "month": "2026-10", "points": 0 }
  ],
  "totalPoints": 365
}
```

`monthlyRewards` is an ordered array (oldest first) that lists every month in the range, including months with 0 points, so a UI can render it directly.

### Errors

Errors use the standard [RFC 7807](https://www.rfc-editor.org/rfc/rfc7807) problem format:

| Case                              | Status |
|-----------------------------------|--------|
| Unknown customer ID               | 404    |
| `startDate` after `endDate`       | 400    |
| Malformed date or ID              | 400    |

```json
{ "type": "about:blank", "title": "Not Found", "status": 404, "detail": "Customer not found: 99", "instance": "/api/rewards/99" }
```

## Sample data

The data lives in `InMemoryTransactionRepository` and is dated relative to the current month, so the default window always has data to show. "M" is the current month.

| Customer           | Transactions                                                    | What it shows                              |
|--------------------|-----------------------------------------------------------------|--------------------------------------------|
| 1 – Alice Johnson  | $120, $75.50 (M-2); $200 (M-1); $45 (M); $500 (M-3)             | Requirement example; the M-3 purchase is excluded |
| 2 – Bob Smith      | $100, $50 (M-2); $51, $100.99 (M-1); $101 (M)                   | Boundary amounts                           |
| 3 – Carol Davis    | $250.75 (M-2); $130 (M)                                         | A month with no purchases shows 0          |
| 4 – David Lee      | $90 (M-3)                                                       | Customer with 0 points in the window       |

Expected totals for the default window: Alice 365, Bob 153, Carol 460, David 0.

## Project structure

```
src/main/java/com/retailer/rewards
├── RewardsApplication.java          Application entry point
├── config/
│   ├── ClockConfig.java             Clock bean, replaced by a fixed clock in tests
│   └── OpenApiConfig.java           Title and description for the Swagger document
├── controller/                      REST endpoints
│   ├── RewardsController.java
│   └── TransactionController.java
├── service/
│   ├── RewardPointsCalculator.java  Points for a single transaction
│   ├── RewardsService.java          Date range, grouping by month, totals
│   └── TransactionService.java
├── repository/                      Data access interfaces + in-memory sample data
├── model/                           Customer, Transaction
├── dto/                             API response shapes
└── exception/                       Custom exceptions and global error handler
```

To use a real database, add an implementation of `CustomerRepository` and `TransactionRepository` (for example with Spring Data JPA). Nothing else needs to change.

## Tests

| Test class                     | Type        | Covers                                                        |
|--------------------------------|-------------|---------------------------------------------------------------|
| `RewardPointsCalculatorTest`   | Unit        | Points for each boundary amount, invalid amounts              |
| `RewardsServiceTest`           | Unit        | Monthly and total points, default window, custom and partial ranges, year end, errors |
| `TransactionServiceTest`       | Unit        | All transactions, filtering by customer, unknown customer      |
| `RewardsApiIntegrationTest`    | Integration | Every endpoint over HTTP, JSON shape, status codes, OpenAPI document |

Tests fix "today" at 2026-10-15 with a `Clock`, so the results don't depend on when they run.

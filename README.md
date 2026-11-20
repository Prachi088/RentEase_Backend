# RentEase Enterprise Backend

Production-grade Java full-stack rental platform backend.

Built with **Java 17**, **Spring Boot 3.3.3**, **Spring Security 6 (stateless JWT)**, **Spring Data JPA / Hibernate**, **Flyway**, and **PostgreSQL 16**.

---

## Architecture Overview

RentEase provides a multi-category rental engine supporting:

| Category | Details |
|---|---|
| Residential Homes & PGs | BHKs, furnishing, gated society status, pet friendly, maintenance |
| Self-Drive Vehicles | Cars, bikes, transmission, fuel type, daily km limits |
| Commercial Workspaces | Managed offices, retail shops, coworking hot-desks |
| Event Venues | Marriage gardens, banquet pavilions, capacity, curfews |

### Package Structure

```text
com.rentease
├── RentEaseApplication.java
├── config/          # CORS, Security, OpenAPI
├── controller/      # REST controllers (/api/v1/*)
├── dto/             # Request & response contracts, ApiResponse<T>
├── entity/          # JPA relational entities
├── enums/           # Domain enums (Roles, Statuses, OwnerTypes)
├── exception/       # Global exception handler & custom errors
├── repository/      # Spring Data JPA repositories
├── security/        # JWT token provider, filter, UserDetailsService
├── service/         # Business logic & PaymentGateway abstraction
└── specification/   # JPA Criteria builder for dynamic multi-filter search
```

---

## Getting Started

### Prerequisites

- Java 17+ (Temurin / OpenJDK)
- Maven 3.8+
- Docker & Docker Compose (optional, for an instant PostgreSQL container)

### Environment Variables

Set these before starting the database and the application. Never commit real values.

| Variable | Purpose |
|---|---|
| `DB_PASSWORD` | PostgreSQL password (used by both the container and the application) |
| `JWT_SECRET` | Strong secret used to sign JWT tokens |
| `GROQ_API_KEY` | Groq API key (keep it in a local `.env`, which is git-ignored) |

### Step 1: Start PostgreSQL

Boot PostgreSQL using the Docker Compose file in the project root:

```bash
docker compose up -d postgres
```

The development profile connects to `jdbc:postgresql://localhost:5433/rentease_db`.

### Step 2: Run the Spring Boot Application

From the backend directory:

```bash
cd backend
mvn clean spring-boot:run
```

Or with the dev profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway automatically applies the schema and reference-data migrations, including supported city and locality records.

The API listens at `http://localhost:8081`.

---

## Interactive OpenAPI & Swagger UI

Once the app is running:

- **Swagger UI:** http://localhost:8081/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8081/api-docs

---

## Account Setup

The migrations seed roles and reference data, but **do not create demo users**.

- Register a Customer or Provider account using `POST /api/v1/auth/register`.
- Administrative accounts must be provisioned through a trusted operator process. Public registration cannot grant admin privileges.

---

## REST API Summary (`/api/v1/*`)

### Authentication

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/auth/login` | Authenticate and obtain a JWT Bearer token |
| POST | `/api/v1/auth/register` | Register as Customer or Provider |

### Inventory & Search

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/listings` | Search and filter across categories (params: `category`, `locality`, `minPrice`, `maxPrice`, `ownerType`, `verifiedOnly`, `keyword`) |
| GET | `/api/v1/listings/{id}` | Detailed specifications of a rental |
| GET | `/api/v1/listings/featured` | Active, verified featured listings |
| POST | `/api/v1/listings` | Publish a new rental (Provider/Broker only) |
| GET | `/api/v1/listings/my` | Current provider's listings |
| GET | `/api/v1/admin/listings/pending` | Listings waiting for admin review |
| POST | `/api/v1/admin/listings/{id}/review` | Approve or reject a listing |

### Bookings & Concurrency Slot Locking

| Method | Endpoint | Description |
|---|---|---|
| POST | `/api/v1/bookings` | Reserve a rental with conflict validation (`409 Conflict` if dates overlap) |
| GET | `/api/v1/bookings/my` | Customer's booking history |
| POST | `/api/v1/bookings/{id}/cancel` | Cancel a booking reservation |

### Administration & Compliance

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/v1/admin/dashboard` | Platform KPIs (active inventory, pending verifications, total users) |
| POST | `/api/v1/admin/verifications/{id}/review` | Approve or reject owner identity or property title deeds |

---

## Docker Database

The Compose file starts **PostgreSQL only**. Run the Spring Boot application separately:

```bash
docker compose up -d postgres
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Configure a strong `JWT_SECRET` and `DB_PASSWORD` in the environment before starting the application.
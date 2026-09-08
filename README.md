# Motoboy Delivery Service (Java Uber-Like App)

Backend for an Uber-like delivery platform for motorcycle couriers ("motoboys"). It exposes a REST API for user management, delivery lifecycle, real-time courier location tracking, and notifications, built with Spring Boot on top of PostgreSQL, secured with JWT-based stateless authentication.

## Tech Stack

- **Java 17**
- **Spring Boot 3.2** (Web, Data JPA, Security, WebSocket, Validation, Actuator, Batch, HATEOAS)
- **Spring Security** — stateless JWT authentication with role-based authorization
- **JJWT 0.11.5** — JWT issuing/parsing (HS256)
- **Spring Data JPA / Hibernate** — persistence, `ddl-auto: update`
- **PostgreSQL 15** — primary datastore
- **Flyway** — on the classpath for schema migrations (currently disabled in the `local` profile in favor of Hibernate auto-DDL)
- **Spring WebSocket + STOMP/SockJS** — real-time delivery/location/notification updates
- **Lombok** — boilerplate reduction
- **Maven** — build and dependency management
- **JUnit 5 / Mockito / Testcontainers / Spring REST Docs** — testing
- **Docker Compose** — local Postgres + pgAdmin

## Architecture

The codebase follows a **modular, feature-based structure** rather than a classic layer-first structure. Each business capability lives under `modules/<name>` and internally follows a light clean-architecture split of `controller` → `service` → `repository`, with `domain` (entities/enums) and `dto` (request/response contracts) kept separate from the persistence model.

```text
src/main/java/com/sbaldasso/combobackend/
├── CombobackendApplication.java        # Spring Boot entry point
├── config/
│   └── WebSocketConfig.java            # STOMP endpoint + broker configuration
└── modules/
    ├── auth/
    │   ├── config/                     # SecurityConfig, JwtConfig
    │   ├── controller/                 # AuthenticationController
    │   ├── dto/                        # AuthenticationRequest/Response
    │   ├── filter/                     # JwtAuthenticationFilter
    │   └── service/                    # AuthenticationService, JwtService, SecurityService
    ├── user/
    │   ├── controller/ domain/ dto/ repository/ service/
    ├── delivery/
    │   ├── controller/ domain/ dto/ repository/ service/
    ├── location/
    │   ├── controller/ domain/ dto/ repository/ service/
    ├── notification/
    │   ├── controller/ domain/ repository/ service/   # includes WebSocketService
    ├── payment/
    │   └── domain/                      # Payment, PaymentMethod, PaymentStatus (data model only, no service/controller yet)
    └── rating/
        └── domain/                      # Rating (data model only, no service/controller yet)
```

### Module responsibilities

| Module | Responsibility |
|---|---|
| `auth` | Login, JWT issuing/validation, Spring Security wiring, request filtering |
| `user` | Account CRUD, user types (customer, courier, admin) |
| `delivery` | Delivery request lifecycle (created → accepted/rejected → in transit → completed) |
| `location` | Courier geolocation updates, persisted and broadcast over WebSocket |
| `notification` | Domain event notifications, delivered via WebSocket and persisted for history |
| `payment` | Payment domain model (method, status) — reserved for a future payment service |
| `rating` | Rating domain model — reserved for a future rating/review service |

## Security Model

- Authentication is **stateless**: `SessionCreationPolicy.STATELESS`, no server-side session state.
- `AuthenticationController` issues a JWT on login; `JwtAuthenticationFilter` validates the bearer token on every subsequent request and populates the `SecurityContext`.
- Authorization uses **Spring Security roles** derived from `UserType` (`ROLE_<USER_TYPE>`), enforced with `@EnableMethodSecurity` for method-level checks and endpoint matchers in `SecurityConfig`.
- Passwords are hashed with **BCrypt**.
- `/v1/auth/**` and Swagger UI endpoints are public; everything else requires a valid JWT.
- CORS is centrally configured in `SecurityConfig` / `application.yml` (`app.cors.allowed-origins`).

## Real-Time Communication

`WebSocketConfig` exposes a STOMP endpoint (with SockJS fallback) used to push:
- delivery status changes,
- courier location updates,
- notifications,

to connected clients without polling.

## How to Run the Project

### 1. Clone the repository

```sh
git clone https://github.com/samuelbaldasso/Java-Uber-Like-App.git
cd Java-Uber-Like-App
```

### 2. Start PostgreSQL

#### Using Docker (recommended)

```sh
docker-compose up -d
```

This starts:
- `postgres` (PostgreSQL 15) on port `5432`, database `motoboy_delivery`
- `pgadmin` (pgAdmin 4) on port `5050` for database inspection

#### Or running locally without Docker

```sh
brew install postgresql
brew services start postgresql
psql postgres
```

```sql
CREATE DATABASE combobackend;
CREATE USER combo_user WITH PASSWORD 'combo_pass';
GRANT ALL PRIVILEGES ON DATABASE combobackend TO combo_user;
```

The `local` Spring profile (`src/main/resources/application-local.yml`) points at that database:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/combobackend
    username: combo_user
    password: combo_pass
    driver-class-name: org.postgresql.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
  flyway:
    enabled: false
```

The default profile (`application.yml`) targets the Docker Compose database (`motoboy_delivery`, user `postgres`) instead.

### 3. Run the backend

```sh
# with the local profile (local PostgreSQL install)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# with the default profile (Docker Compose PostgreSQL)
./mvnw spring-boot:run
```

The API is served under the `/api` context path on port `8080` (see `application.yml`).

### 4. Run the tests

```sh
./mvnw test -Dspring.profiles.active=local
```

Repository and service tests use **Testcontainers**, so a running Docker daemon is required.

## API Documentation

The API is documented with **Swagger/OpenAPI**. After starting the backend, visit:

```text
http://localhost:8080/api/swagger-ui.html
```

## Architecture Decision Records (ADRs)

### ADR-001 — Feature-based modular structure over layer-first packages
- **Status**: Accepted
- **Context**: A layer-first layout (`controller/`, `service/`, `repository/` at the project root) scales poorly as the number of business domains grows — related classes for a single feature end up scattered across distant packages.
- **Decision**: Organize code by business capability under `modules/<feature>`, with each module internally split into `controller`, `service`, `repository`, `domain`, and `dto`.
- **Consequences**: Feature boundaries are easy to navigate and extract later (e.g., into separate services); some cross-cutting concerns (e.g., shared DTOs) require conscious placement decisions.

### ADR-002 — Stateless JWT authentication instead of session-based auth
- **Status**: Accepted
- **Context**: The API needs to serve both a web client and a WebSocket-connected mobile/courier client. Server-side sessions would require sticky sessions or a shared session store to scale horizontally.
- **Decision**: Use JWT (HS256, via JJWT) with `SessionCreationPolicy.STATELESS`. Tokens are issued by `AuthenticationService`/`JwtService` and validated per-request by `JwtAuthenticationFilter`.
- **Consequences**: Any instance can validate a request without shared state, simplifying horizontal scaling. Token revocation before expiry is not built in — logout is client-side, and long-lived tokens are mitigated with a 24h expiration (`security.jwt.expiration`).

### ADR-003 — Role-based authorization via Spring Security, driven by `UserType`
- **Status**: Accepted
- **Context**: The platform has distinct actor types (customer, courier, admin) with different permitted operations.
- **Decision**: Map `UserType` to a Spring Security `GrantedAuthority` (`ROLE_<USER_TYPE>`) at `UserDetailsService` load time, and enforce authorization declaratively with `@EnableMethodSecurity` and endpoint matchers rather than manual checks in controllers.
- **Consequences**: Authorization rules are centralized and testable; adding a new role only requires extending the `UserType` enum and annotating the relevant endpoints/methods.

### ADR-004 — WebSocket (STOMP over SockJS) for real-time updates
- **Status**: Accepted
- **Context**: Delivery status, courier location, and notifications need to reach clients with low latency; client-side polling would add load and latency.
- **Decision**: Use Spring's STOMP-over-WebSocket support (`WebSocketConfig`), with SockJS as a fallback for environments where raw WebSockets are blocked.
- **Consequences**: Real-time UX without polling; requires clients to maintain a persistent connection and handle reconnect/backoff logic.

### ADR-005 — PostgreSQL with Hibernate `ddl-auto: update`, Flyway on standby
- **Status**: Accepted (with a known follow-up)
- **Context**: Early-stage schema iterates quickly; a rigid migration-first workflow would slow local development, but production environments still need reproducible, auditable schema changes.
- **Decision**: Use Hibernate's `ddl-auto: update` for local/dev convenience today. Flyway is already a dependency and disabled (`spring.flyway.enabled: false`) so it can be switched on to take ownership of schema migrations as the schema stabilizes.
- **Consequences**: Fast local iteration now; before any production deployment, migrations should be authored under `src/main/resources/db/migration` and `ddl-auto` should move to `validate` to avoid uncontrolled schema drift.

### ADR-006 — Docker Compose for local infrastructure only
- **Status**: Accepted
- **Context**: The application itself doesn't need to run in Docker for local development, but a consistent, disposable PostgreSQL instance does.
- **Decision**: `docker-compose.yml` provisions only PostgreSQL and pgAdmin. The Spring Boot application runs directly via Maven (`./mvnw spring-boot:run`), not as a container, to keep the local dev loop (hot reload via `spring-boot-devtools`) fast.
- **Consequences**: Fast iteration locally; a separate `Dockerfile` and deployment pipeline will be needed to containerize the application itself for staging/production.

### ADR-007 — `payment` and `rating` modules scaffolded as domain-only
- **Status**: Accepted (partial implementation)
- **Context**: Payments and ratings are known future requirements, and their data shape influences the `delivery` domain (e.g., a delivery references a payment method/status).
- **Decision**: Model `Payment`, `PaymentMethod`, `PaymentStatus`, and `Rating` as domain entities now, without corresponding services, controllers, or repositories, so the shape is fixed early without committing to business logic prematurely.
- **Consequences**: These modules are not yet functional end-to-end; implementing `service`/`repository`/`controller` layers for them is tracked as future work (see below).

## Known Gaps / Next Steps

- Flyway migrations are not yet authored; schema is currently managed via Hibernate auto-DDL.
- `payment` and `rating` modules lack service, repository, and controller layers.
- CORS allowed origins (`*` in `SecurityConfig`, a single origin in `application.yml`) should be reconciled and tightened before production use.
- JWT secret in `application.yml` is a placeholder and must be externalized (e.g., environment variable or secrets manager) for any non-local environment.

## How to Contribute

1. Fork the project.
2. Create a feature branch (`git checkout -b feature-new`).
3. Commit your changes (`git commit -m 'Add new feature'`).
4. Push the branch (`git push origin feature-new`).
5. Open a Pull Request.

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

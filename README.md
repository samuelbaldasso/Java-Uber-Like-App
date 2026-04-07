# Uber-Like App Backend

This repository contains the backend of an Uber-like app for motorcycle couriers, developed with Java and Spring Boot, using RBAC, JWT for security, WebSockets for real-time communication, Docker for containerization, PostgreSQL for persistence, and Maven for dependency management, following clean architecture.

## Technologies Used

- Java 17
- Spring Boot 3
- Spring Security (JWT and RBAC)
- Spring WebSockets
- Spring Data JPA
- PostgreSQL
- Docker
- Maven
- Clean Architecture
- JUnit5 / Mockito

## Main Features

- **Authentication and Authorization**: JWT with RBAC for role-based access control (user, courier, admin).
- **Profile Management**: Editing and viewing profiles.
- **Geolocation and Mapping**: Integration for location selection.
- **Race Request**: Creation, acceptance/rejection of requests.
- **Real-Time Update**: WebSockets for status tracking.

## How to Run the Project

### 1. Clone the Repository

```sh
 git clone https://github.com/samuelbaldasso/Java-Uber-Like-App.git
 cd Java-Uber-Like-App
```

### 2. Configure the Database

#### Using Docker (recommended)

```sh
docker-compose up -d
```

#### Or running locally (without Docker)

1. Install PostgreSQL (e.g.: `brew install postgresql`)
2. Start the service: `brew services start postgresql`
3. Create the database and user:

   ```sh
   psql postgres
   ```

   ```sql
   CREATE DATABASE combobackend;
   CREATE USER combo_user WITH PASSWORD 'combo_pass';
   GRANT ALL PRIVILEGES ON DATABASE combobackend TO combo_user;
   ```

4. The project already has the file `src/main/resources/application-local.yml` with the configuration to run locally:

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

### 3. Running the Backend

#### With Docker

```sh
docker-compose up -d
```

#### Locally (without Docker)

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 4. Running the Tests

```sh
./mvnw test -Dspring.profiles.active=local
```

## API documentation

The API is documented with **Swagger**. After starting the backend, go to:

```text
http://localhost:8080/swagger-ui.html
```

## Project Structure

```text
UberLike/
├── src/main/java/com/uberlike/
│   ├── config/ # General system settings
│   ├── controller/ # REST API controllers
│   ├── service/ # Services and business rules
│   ├── repository/ # JPA repositories
│   ├── security/ # Security and JWT configuration
│   ├── model/ # Entities and models
│   └── websocket/ # WebSockets configuration and service
├── src/main/resources/
│   ├── application.yml # Main configurations
│   ├── application-local.yml # Local configuration
│   ├── db/migration/ # Database migration scripts
├── Dockerfile
├── docker-compose.yml
├── pom.xml
└── README.md
```

## How to contribute

1. Fork the project.
2. Create a branch with the feature (`git checkout -b feature-new`).
3. Commit your changes (`git commit -m 'Add new feature'`).
4. Push to the branch (`git push origin feature-new`).
5. Open a Pull Request.

## License

This project is licensed under the MIT license. See the `LICENSE` file for more details.

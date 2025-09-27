# PayLite API

PayLite is a lightweight, robust, and secure RESTful API for processing payments. It provides endpoints for creating and retrieving payments, and for handling webhooks from payment service providers (PSPs).

## Features

*   **Create Payment Intents**: Initiate a payment by providing an amount, currency, and customer details.
*   **Retrieve Payments**: Get the status and details of a specific payment.
*   **Idempotent Requests**: Safely retry `POST` requests without accidentally performing the same operation twice.
*   **Webhook Handling**: Securely process incoming webhooks from PSPs to update payment statuses.
*   **API Key Authentication**: Secure your API with API keys.
*   **Database Migrations**: Uses Flyway for version-controlled database schema management.
*   **Docker Support**: Run the entire application and its database in Docker containers.
*   **OpenAPI Documentation**: Interactive API documentation powered by Springdoc OpenAPI.
*   **Efficient Docker Builds**: Utilizes a multi-stage Dockerfile to create a small, optimized runtime image.

## Technologies Used

*   **Java 21**: The core programming language.
*   **Spring Boot 3**: Framework for building the application.
*   **Spring Data JPA**: For database interaction.
*   **Spring Web**: For building the RESTful API.
*   **MySQL**: The relational database for production.
*   **H2 Database**: In-memory database for testing.
*   **Flyway**: For database migrations.
*   **Lombok**: To reduce boilerplate code.
*   **Springdoc OpenAPI**: For generating API documentation.
*   **Maven**: For dependency management and building the project.
*   **Docker**: For containerization.

## Prerequisites

*   Java 21 or later
*   Maven 3.6 or later
*   Docker and Docker Compose (for running with Docker)
*   A MySQL database (if not using Docker)

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/richynick/paylite_repo
cd payLite
```

### 2. Configure the database

If you are not using the provided Docker setup, you will need to configure the application to connect to your own MySQL database.

Open the `src/main/resources/application.properties` file and update the following properties:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/paylite
spring.datasource.username=your-mysql-username
spring.datasource.password=your-mysql-password
```

### 3. Build and run the application

You can build and run the application using Maven:

```bash
./mvnw spring-boot:run
```

The application will start on port `8080`.

## API Documentation

Once the application is running, you can access the interactive API documentation at:

[http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## API Endpoints

### Authentication

All API endpoints are secured with API key authentication. You must provide a valid API key in the `X-API-Key` header of your requests.

A default API key `test-api-key` is configured in `application.properties`.

### Payments

#### `POST /api/v1/payments`

Creates a new payment intent. This endpoint is idempotent, meaning you can safely retry the request in case of a network error. To do so, you must provide a unique `Idempotency-Key` in the request header.

**Headers**

*   `Idempotency-Key`: A unique key for the request.

**Request Body**

```json
{
  "amount": 100.00,
  "currency": "USD",
  "customerEmail": "customer@example.com",
  "reference": "order-123"
}
```

**Response**

```json
{
  "paymentId": "pi_...",
  "status": "PENDING",
  "amount": 100.00,
  "currency": "USD",
  "customerEmail": "customer@example.com",
  "reference": "order-123"
}
```

#### `GET /api/v1/payments/{paymentId}`

Retrieves the details of a specific payment.

**Path Parameters**

*   `paymentId`: The ID of the payment to retrieve.

**Response**

```json
{
  "paymentId": "pi_...",
  "status": "SUCCEEDED",
  "amount": 100.00,
  "currency": "USD",
  "customerEmail": "customer@example.com",
  "reference": "order-123",
  "createdAt": "2025-09-26T10:00:00Z"
}
```

### Webhooks

#### `POST /api/v1/webhooks/psp`

HHandles incoming webhooks from a Payment Service Provider (PSP). The endpoint verifies the signature of the webhook to ensure it's authentic.

**Headers**

*   `X-PSP-Signature`: The signature of the webhook payload.

**Request Body**

```json
{
  "paymentId": "pi_...",
  "event": "payment.succeeded"
}
```

**Response**

A `200 OK` response with an empty body.

## Idempotency

The `POST /api/v1/payments` endpoint supports idempotency. If you make a request with an `Idempotency-Key` that has been used before with the same request body, the original response will be returned without creating a new payment. If the `Idempotency-Key` is the same but the request body is different, a `409 Conflict` error will be returned.

## Webhook Security

The `POST /api/v1/webhooks/psp` endpoint is secured by verifying the signature of the incoming webhook. The signature is expected in the `X-PSP-Signature` header. The signature is a HMAC-SHA256 hash of the raw request body, using a secret key configured in the application.

The default webhook secret is `super-secret-webhook-key` and can be configured in `application.properties`.

## Running Tests

To run the unit and integration tests, use the following Maven command:

```bash
./mvnw test
```

## Running with Docker

The project includes a `docker-compose.yml` file to easily run the application and a MySQL database in Docker containers.

### Prerequisites

*   Docker
*   Docker Compose

### Usage

1.  **Build and start the containers**:

    ```bash
    docker-compose up --build
    ```

The `--build` flag tells Docker Compose to build the image using the `Dockerfile`.

The application will be available at `http://localhost:8082`.

To stop and remove the containers, run:

```bash
docker-compose down
```

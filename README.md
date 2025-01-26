# Spring Boot RabbitMQ STOMP WebSocket Multi-tenancy Demo

Simple example demonstrating multi-tenant WebSocket messaging using Spring Boot, RabbitMQ (STOMP), and Redis for persistence.

## Features

- Multi-tenant WebSocket communication via RabbitMQ STOMP
- Message persistence in Redis
- Web client for testing multiple tenant connections
- Message forwarding on different topics including the sent userId's
- Broadcasting Service which broadcasts system messages to the different tenants periodically on the news topic

## Prerequisites

- Docker and Docker Compose
- Java 17
- Gradle

## Setup

1. Start services:
```bash
docker-compose up -d
```

2. Run application:
```bash
./gradlew bootRun
```

3. Open the test clients index.html using VS Code's Live Server extension (127.0.0.1:5500)
By opening multiple clients e. g. in two different browsers/windows you can test sending/receiving messages on the different tenants.

## Management UIs

- RabbitMQ: http://localhost:15672 (guest/guest)
- Redis Commander: http://localhost:8081

## Testing

1. Open web client
2. Connect to both tenants
3. Watch messages in the UI
4. Check Redis Commander for stored messages
5. Monitor RabbitMQ management UI for connections
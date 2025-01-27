# Spring Boot RabbitMQ STOMP WebSocket Multi-tenancy Demo

Simple example demonstrating multi-tenant WebSocket messaging using Spring Boot, RabbitMQ (STOMP), and Redis for persistence.

## Features

- Multi-tenant WebSocket communication via RabbitMQ STOMP
- Message persistence in Redis
- Web client for testing multiple tenant connections
- Message forwarding on different topics including the sent userId's
- Broadcasting Service which broadcasts system messages to the different tenants periodically on the news topic

## How Multitenancy is handled

### Connection Establishment

When a client establishes a WebSocket connection to the server, it sends an initial `CONNECT` frame that includes an `authorization` header. This header typically contains the tenant identifier in the format `Bearer {tenant}`. 

Upon receiving the `CONNECT` command, the server processes the authorization header in the `TenantInterceptor`. The tenant identifier is extracted and stored as a session attribute for the duration of the connection. This allows the server to maintain the context of the tenant throughout the session.

In real life examples this would be a base64 encoded JWT token which holds much more information, but the principle would be the same. You'd extract whatever attribute holds the tenant information. Before accepting a connection, the token would have to be validated which we skip in this scenario, but should be part of the interceptor. 
In error cases the connection needs to be rejected.

### Message Processing

During message processing, when a client sends a message to a specific topic, the server retrieves the tenant identifier from the session attributes. This enables the server to determine which broker connection should process the incoming message.

By accessing the tenant session attribute, the server can efficiently route messages to the appropriate broker, ensuring that each tenant's messages are handled correctly and securely. This design eliminates the need for clients to resend the tenant identifier with each message, simplifying the message structure and improving overall performance.
Clients cannot directly manipulate session attributes of a standing connection; this is solely the responsibility of the server (broker), which makes this a solid approach.

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
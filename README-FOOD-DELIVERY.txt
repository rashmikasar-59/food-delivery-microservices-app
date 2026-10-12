# Food Delivery Microservices

A Java 17 and Spring Boot microservices application inspired by food delivery platforms such as Swiggy and Zomato. The project demonstrates service discovery, API Gateway routing, JWT authentication, role-based authorization, MongoDB persistence, Kafka event messaging, and Docker containerization.

> **Project status:** Learning and demonstration project. Verify the application configuration and API responses in your environment before deployment.

## Architecture

```mermaid
flowchart TD
    Client[Postman / Client] --> Gateway[API Gateway :8080]

    Gateway --> Auth[Auth Service]
    Gateway --> User[User Service]
    Gateway --> Restaurant[Restaurant Service]
    Gateway --> Order[Order Service]
    Gateway --> Payment[Payment Service]
    Gateway --> Delivery[Delivery Service]

    Auth --> Eureka[Eureka Server :8761]
    User --> Eureka
    Restaurant --> Eureka
    Order --> Eureka
    Payment --> Eureka
    Delivery --> Eureka
    Gateway --> Eureka

    Auth --> Mongo[(MongoDB)]
    User --> Mongo
    Restaurant --> Mongo
    Order --> Mongo
    Payment --> Mongo
    Delivery --> Mongo

    Order -. order notification .-> Kafka[Apache Kafka]
    Kafka -. consumes event .-> Restaurant
```

The diagram shows the intended architecture. Actual communication paths, event publishing, and topic configuration depend on the application source code.

## Services

| Service | Port / endpoint | Responsibility |
|---|---|---|
| Discovery Server | `8761` | Eureka service registry |
| API Gateway | `8080` | Central API entry point and JWT validation |
| Auth Service | `8081` | Registration, login, and JWT issuance |
| User Service | Dynamic locally; configure a fixed container port | User profiles and management |
| Restaurant Service | Dynamic locally; configure a fixed container port | Restaurants and food items |
| Order Service | Dynamic locally; configure a fixed container port | Order creation and status workflow |
| Payment Service | Dynamic locally; configure a fixed container port | Demonstration payment workflow |
| Delivery Service | Dynamic locally; configure a fixed container port | Delivery workflow |

**Note:** If a service uses `server.port=0`, Spring Boot selects an available port. For Docker deployment, configure a fixed internal port for each service where appropriate. `EXPOSE` in a Dockerfile does not configure Spring Boot's listening port.

## Technology Stack

- Java 17
- Spring Boot
- Spring Security and JWT
- Spring Cloud Gateway
- Netflix Eureka
- MongoDB
- Apache Kafka and Spring for Apache Kafka
- Maven
- Docker and Docker Compose
- Postman
- Eclipse / Spring Tool Suite (STS)

## Prerequisites

For local development:

- JDK 17
- Maven
- MongoDB
- Apache Kafka
- Postman

For containerized deployment:

- Docker Desktop or Docker Engine
- Docker Compose v2

Verify the installations:

```bash
java -version
mvn -version
docker --version
docker compose version
```

## Project Structure

```text
food-delivery-microservices/
├── pom.xml
├── discovery-server/
│   ├── pom.xml
│   └── Dockerfile
├── api-gateway/
│   ├── pom.xml
│   └── Dockerfile
├── auth-service/
│   ├── pom.xml
│   └── Dockerfile
├── user-service/
│   ├── pom.xml
│   └── Dockerfile
├── restaurant-service/
│   ├── pom.xml
│   └── Dockerfile
├── order-service/
│   ├── pom.xml
│   └── Dockerfile
├── payment-service/
│   ├── pom.xml
│   └── Dockerfile
├── delivery-service/
│   ├── pom.xml
│   └── Dockerfile
├── docker-compose.yml
├── .env.example
└── README.md
```

This is the intended layout. Keep only files and directories that actually exist in your repository.

## Java 17 Docker Configuration

Each service uses a Java 17-compatible Docker image. The recommended pattern is a multi-stage build:

1. Build the Spring Boot application with Maven and JDK 17.
2. Run the packaged JAR in a Java 17 JRE image.
3. Keep the runtime image separate from the Maven build environment.

### User Service Dockerfile

The User Service Dockerfile follows this pattern:

```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml ./pom.xml
COPY user-service/pom.xml ./user-service/pom.xml

# Copy the root project and sibling modules for the Maven reactor.
COPY . .

RUN mvn -pl user-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /workspace/user-service/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
```

**How it works:**

- `maven:3.9-eclipse-temurin-17` supplies Maven and JDK 17 for compilation.
- `mvn -pl user-service -am -DskipTests package` builds User Service and its required reactor modules.
- `eclipse-temurin:17-jre` provides the runtime environment.
- `COPY --from=build` copies the packaged JAR from the build stage.
- `EXPOSE 8080` documents the intended container port.
- `MaxRAMPercentage=75.0` sets a JVM heap sizing limit relative to the memory available to the JVM.

The root Maven POM must declare the required modules. The Docker build context must be the project root because the Dockerfile copies the parent POM and sibling modules.

### Maven Java Version

Check the parent `pom.xml` and ensure the project targets Java 17:

```xml
<properties>
    <java.version>17</java.version>
    <maven.compiler.release>17</maven.compiler.release>
</properties>
```

Avoid adding duplicate properties if they already exist.

### Dockerfile Requirements for All Services

Each microservice should have its own Dockerfile, adjusted for its module path:

- `discovery-server/Dockerfile`
- `api-gateway/Dockerfile`
- `auth-service/Dockerfile`
- `user-service/Dockerfile`
- `restaurant-service/Dockerfile`
- `order-service/Dockerfile`
- `payment-service/Dockerfile`
- `delivery-service/Dockerfile`

Use the appropriate Maven module name and generated JAR path in each Dockerfile. Ensure the runtime port matches the service's Spring Boot configuration.

## Environment Variables

Create a local `.env` file if your Docker Compose configuration reads these variables.

```dotenv
JWT_SECRET_BASE64=YOUR_GENERATED_BASE64_SECRET
MONGODB_HOST=mongodb
MONGODB_PORT=27017
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
EUREKA_SERVER_URL=http://discovery-server:8761/eureka
```

These values are examples. The variable names and service hostnames must match the application's actual configuration and Compose file.

Generate a development JWT secret using OpenSSL:

```bash
openssl rand -base64 32
```

Set the generated value in `JWT_SECRET_BASE64`. The authentication service and all components that validate its JWTs must use compatible signing and verification settings.

If your application uses a different variable name or requires a specific secret format, follow the implementation in the source code.

Never commit `.env`, passwords, JWT secrets, or production credentials. Add `.env` to `.gitignore` and commit only a sanitized `.env.example`.

## Import into Eclipse / STS

1. Extract the project ZIP.
2. Open Eclipse or Spring Tool Suite.
3. Select **File → Import → Maven → Existing Maven Projects**.
4. Select the project root containing `pom.xml`.
5. Import all detected Maven projects.
6. Wait for dependencies to download.
7. Confirm that the IDE uses JDK 17.
8. Verify that environment variables are configured for launched applications.

## Run Locally Without Docker

Start MongoDB and Kafka before starting the Spring Boot services.

Build the Maven reactor from the root folder:

```bash
mvn clean verify
```

Start the services in this order:

1. Discovery Server
2. Auth Service
3. User Service
4. Restaurant Service
5. Order Service
6. Payment Service
7. Delivery Service
8. API Gateway

Start each module using its main application class or:

```bash
mvn spring-boot:run
```

Run the command from the appropriate module directory.

Open the Eureka dashboard:

`http://localhost:8761`

Use the Gateway for API testing:

`http://localhost:8080`

## Run with Docker

Run all commands below from the project root containing `docker-compose.yml`.

### 1. Build all service images

```bash
docker compose build
```

To force a fresh image build:

```bash
docker compose build --no-cache
```

### 2. Start the application

```bash
docker compose up -d
```

This starts the services and infrastructure defined in your Compose file. MongoDB and Kafka will start automatically only if they are included in that file.

### 3. Check running containers

```bash
docker compose ps
```

### 4. View application logs

```bash
docker compose logs -f
```

View a specific service:

```bash
docker compose logs -f api-gateway
docker compose logs -f discovery-server
docker compose logs -f auth-service
docker compose logs -f user-service
docker compose logs -f restaurant-service
docker compose logs -f order-service
docker compose logs -f payment-service
docker compose logs -f delivery-service
```

Use the exact service names from `docker-compose.yml`.

### 5. Verify the application

If the corresponding ports are published by Compose, open:

- Eureka: `http://localhost:8761`
- API Gateway: `http://localhost:8080`

Check that the application services have registered with Eureka and that the Gateway can reach their configured destinations.

### 6. Rebuild after code changes

```bash
docker compose up -d --build
```

### 7. Stop the application

```bash
docker compose down
```

This stops and removes the containers but normally preserves named volumes.

To remove Compose-managed volumes as well:

```bash
docker compose down -v
```

**Warning:** Removing volumes can permanently delete persisted MongoDB data.

## Build an Individual Docker Image

For example, build the User Service image from the project root:

```bash
docker build -f user-service/Dockerfile -t food-delivery-user-service .
```

Check the generated image:

```bash
docker images
```

Run this command only if the image is not already managed through Compose. In a Compose deployment, prefer:

```bash
docker compose build user-service
docker compose up -d user-service
docker compose logs -f user-service
```

The Compose service name must match the actual name in the YAML file.

## Docker Networking

Inside Docker, `localhost` refers to the current container. It does not refer to other containers.

For example, when the Compose service names match these examples:

| Dependency | Container-to-container address |
|---|---|
| MongoDB | `mongodb:27017` |
| Kafka | `kafka:9092` |
| Eureka | `http://discovery-server:8761/eureka` |

Use `localhost:27017` and `localhost:9092` for applications running directly on your computer only when the infrastructure publishes those ports to the host.

For Spring Cloud Gateway and Eureka, confirm that service IDs, routes, and registry URLs match the actual application configuration.

## API Gateway Routes

The documented Gateway routes are:

| Route prefix | Destination |
|---|---|
| `/api/v1/auth/**` | Auth Service |
| `/api/v1/user/**` | Auth Service in the current configuration |
| `/api/v1/restaurant/**` | Restaurant Service |
| `/api/v1/fooditem/**` | Restaurant Service |
| `/api/v1/order/**` | Order Service |
| `/api/v1/payment/**` | Payment Service |
| `/api/v1/delivery/**` | Delivery Service |

Verify these mappings against the current Gateway configuration. The separate User Service does not automatically receive `/api/v1/user/**` requests if the Gateway route targets Auth Service.

## API Testing with Postman

Set a collection variable:

```text
baseUrl = http://localhost:8080
```

Use the request paths and JSON fields defined by the project's controllers and DTOs.

### Recommended testing sequence

1. Register a customer using the Auth Service registration endpoint.
2. Log in through the token endpoint and copy the JWT.
3. Call protected endpoints using the JWT.
4. Test restaurant and food-item operations with an appropriately authorized account.
5. Place an order using valid restaurant and food-item IDs.
6. Exercise the demonstration payment workflow.
7. Test delivery operations using the required role.
8. Verify the Kafka order notification.
9. Test missing tokens, invalid tokens, and insufficient roles.

Add the token in Postman under **Authorization → Bearer Token**.

The required HTTP methods, payloads, permissions, and successful response bodies must be taken from the project's actual API implementation.

## JWT Authentication and Roles

The intended role model includes:

- `CUSTOMER` — normal customer registration, when configured as the default.
- `RESTAURANT_OWNER` — restaurant management.
- `DELIVERY_AGENT` — delivery workflows.
- `ADMIN` — administrative operations.

Public registration should not allow a user to grant themselves administrator privileges. If the project uses a pre-created administrator, follow the application's schema and password-encoding requirements.

After a user's role changes, log in again if the role is embedded in the JWT. A missing or invalid token can result in `401 Unauthorized`; an authenticated user without sufficient permissions can receive `403 Forbidden`.

## Kafka Order Notification

The Order Service publishes an event to:

```text
order-rest-notification-topic
```

The Restaurant Service consumes the event and logs the received order information. In the current workflow, the notification is associated with the successful-payment branch of order processing; simply creating an order may not publish it.

Check the relevant logs:

```bash
docker compose logs -f order-service
docker compose logs -f restaurant-service
docker compose logs -f kafka
```

Verify the broker address, topic name, event serialization, and consumer configuration. Containerized applications must use a broker address reachable from their Docker network.

## Troubleshooting

| Problem | Recommended check |
|---|---|
| Docker build fails | Check Java 17, Maven reactor modules, Docker build context, and JAR paths. |
| Gateway returns 503 | Verify service health, Eureka registration, and Gateway routes. |
| MongoDB connection fails | Check the container status, host name, port, and database configuration. |
| Kafka connection fails | Check broker health and the internal bootstrap address. |
| JWT returns 401 | Check the token, expiration, secret, and signing configuration. |
| Request returns 403 | Check user roles and endpoint authorization rules. |
| Service is missing from Eureka | Check the registry URL and application logs. |
| Container repeatedly exits | Run `docker compose logs <service-name>`. |
| Port is already in use | Change the host port mapping or stop the conflicting process. |
| Java release error | Ensure the Maven compiler and Docker images target Java 17. |

For a Maven build without running tests:

```bash
mvn clean package -DskipTests
```



# Food Delivery Microservices — Full Setup, Workflow & Postman API Testing

A Java 17 / Spring Boot microservices demo for a food-delivery platform. This guide explains how to start the services, test the main customer journey, test role-based authorization, and understand expected responses.

> **Important testing note:** the response examples below show the expected response *shape* and common status codes. IDs, order numbers, timestamps, generated JWTs, and success-message wording can vary with the actual code/data. The ZIP was checked for archive integrity and POM XML structure, but it has **not been compiled or integration-tested in this environment**. Run the build locally and treat the actual Postman response as authoritative.

## Kafka order notification flow and test

The Order Service publishes a JSON event to the Kafka topic `order-rest-notification-topic` after the payment-success branch in `updateOrderAfterPayment()`. The Restaurant Service now consumes that topic with `OrderNotificationListener`. The listener logs the received order ID, user ID, food-item IDs, quantities, and address; it is a verification listener and does not yet create a restaurant preparation record.

### Required configuration

Both Order Service and Restaurant Service must connect to the same Kafka broker. The default local setting is `localhost:9092`; override it with `spring.kafka.bootstrap-servers` if your broker uses another address. Both services include the `spring-kafka` dependency. The producer maps the event type to the `event` JSON type alias, and the Restaurant Service maps that alias to its local `OrderPlacedNotification` class.

### Test the event end to end

1. Start Kafka and verify the broker is listening at `localhost:9092`.
2. Start Discovery Server, Restaurant Service, Order Service, and the other services required for the normal order/payment flow.
3. Register/login and obtain a JWT as described in the API testing sections above.
4. Create an order through the API Gateway and complete the project's successful payment workflow. The current code sends the restaurant notification only after the payment callback reports `SUCCESS`; simply creating an order does not publish this notification yet.
5. Check the Restaurant Service console. Expected log format (generated IDs and values will differ):

```text
Kafka order notification received. orderId=<generated-order-id>, userId=<customer-id>, foodItemIds=[<food-item-id>], quantities=[2], address=Address(...)
```

### Troubleshooting

- If no log appears, confirm the payment-success callback reached `OrderService.updateOrderAfterPayment()` and that both services use the same topic and broker.
- If the consumer reports a JSON/type error, confirm `spring.kafka.producer.properties.spring.json.type.mapping` in Order Service and `spring.kafka.consumer.properties.spring.json.type.mapping` in Restaurant Service use the same alias (`event`).
- If Kafka connection fails, start the broker or correct `spring.kafka.bootstrap-servers`.
- Kafka delivery can be repeated in failure/retry scenarios. When adding real business actions, make the consumer idempotent using the event/order ID.


## 1. Services and ports

| Service | Port | Purpose |
|---|---:|---|
| Discovery Server | `8761` | Eureka service registry |
| API Gateway | `8080` | Public entry point and JWT validation |
| Auth Service | `8081` | Registration, login, token validation, role source of truth |
| User Service | random (`server.port=0`) | Separate user-profile service |
| Restaurant Service | random | Restaurant and food-item APIs |
| Order Service | random | Place orders and update order status |
| Payment Service | random | Demo payment processing |
| Delivery Service | random | Delivery workflow |

Services using `server.port=0` get a random port. Call them through the Gateway at `http://localhost:8080`; do not guess their direct ports. Check Eureka at `http://localhost:8761` to see registered services.

## 2. Prerequisites

Install and verify:

- JDK 17 (`java -version`)
- Maven (`mvn -version`)
- MongoDB running on `localhost:27017`, unless configured otherwise
- Kafka reachable at `localhost:9092` for order/payment event features
- Eclipse IDE / Spring Tool Suite (STS), or another Java IDE
- Postman

### JWT secret

Auth Service signs JWTs; Gateway and User Service validate them. Set the **same** Base64-encoded secret in all three processes as `JWT_SECRET_BASE64`. Generate a development secret in Git Bash:

```bash
openssl rand -base64 32
```

For Eclipse/STS, open each application's **Run Configurations → Environment** and add `JWT_SECRET_BASE64` with the same generated value. Do not commit a real secret to Git.

### Optional admin bootstrap

The project does not intentionally create a default admin account. To bootstrap one for local testing, set these environment variables on the Auth Service launch configuration before its first start:

```text
BOOTSTRAP_ADMIN_USERNAME=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=Use-A-Unique-Long-Password
```

Use your own unique password. Bootstrap does not overwrite an existing username. Once the admin record exists, remove/unset the bootstrap password variable. If the admin already exists, changing the variable will not reset its password.

### Environment variables (optional)

```text
JWT_SECRET_BASE64=<same-generated-base64-secret-in-auth-gateway-user-service>
MONGODB_HOST=localhost
MONGODB_PORT=27017
# Each service has its own default database; override MONGODB_DATABASE only intentionally.
EUREKA_SERVER_URL=http://localhost:8761/eureka
BOOTSTRAP_ADMIN_USERNAME=admin@example.com
BOOTSTRAP_ADMIN_PASSWORD=<local-only-secret>
```

## 3. Build and import

1. Extract the ZIP into a new folder.
2. In STS: **File → Import → Maven → Existing Maven Projects**.
3. Select the root folder containing this root `pom.xml`; import all detected modules.
4. Wait for Maven dependency resolution to finish.
5. In a terminal opened at the root folder, run:

```bash
mvn clean package -DskipTests
```

If Maven reports an error, fix the **first** error in the console before proceeding. This README does not claim a successful compile until this command passes on your machine.

## 4. Start the services in order

1. Start MongoDB.
2. Start Kafka (required for the event-driven flows).
3. Start `DiscoveryServerApplication`.
4. Start `AuthServiceApplication`.
5. Start `UserServiceApplication`.
6. Start `RestaurantServiceApplication`.
7. Start `OrderServiceApplication`.
8. Start `PaymentServiceApplication`.
9. Start `DeliveryServiceApplication`.
10. Start `ApiGatewayApplication` last.

Wait for each application's `Started ...` message in the console. If a service fails, check MongoDB/Kafka connectivity, the shared JWT secret, and Eureka registration before testing APIs.

Health checks (if Actuator is available for the service):

```http
GET http://localhost:8080/actuator/health
GET http://localhost:8761
```

The exact Actuator path availability depends on Gateway routes/configuration; if Gateway does not route `/actuator/**`, open the relevant service's own port from its startup log.

## 5. API testing conventions

- Base URL: `http://localhost:8080`
- In Postman, use **Body → raw → JSON** for JSON requests.
- Public auth endpoints do not require a token.
- For protected APIs, use **Authorization → Bearer Token** and paste the JWT returned by login. Or add header `Authorization: Bearer <JWT>`.
- Never manually send `loggedInUser` or `loggedInRole`. The Gateway removes client-supplied values and derives identity from a valid JWT.
- A `401 Unauthorized` usually means the token is missing/invalid/expired or the shared JWT secret does not match.
- A `403 Forbidden` usually means the token is valid but the user's role is not allowed.

## 6. End-to-end test flow

Recommended order:

1. Bootstrap or prepare the admin.
2. Register a customer.
3. Log in as the customer and save the JWT.
4. Register a restaurant-owner account; admin assigns `RESTAURANT_OWNER`.
5. Log in again as that owner to obtain a JWT containing the updated role.
6. Owner creates a restaurant and food items.
7. Customer lists restaurants and food items.
8. Customer places an order.
9. Customer submits a demo payment.
10. Admin assigns `DELIVERY_AGENT` to another account; that user logs in again.
11. Delivery agent invokes the delivery endpoint.
12. Verify that a customer cannot call owner/admin/delivery-only APIs.

**Important:** role changes are reflected in newly issued JWTs. After the admin changes a user's role, log in as that user again and use the new token.

## 7. Postman requests with expected results

### API 1 — Register customer

**Request**

```http
POST http://localhost:8080/api/v1/user
Content-Type: application/json
```

```json
{
  "fullName": "Test Customer",
  "email": "customer1@example.com",
  "password": "Customer-Pass-123!",
  "phoneNumber": 9876543210,
  "address": {
    "address": "12 Sample Road",
    "city": "Pune",
    "state": "Maharashtra",
    "zipcode": 411001
  },
  "userRole": "ADMIN"
}
```

The submitted `userRole` is deliberately not trusted; public registration stores this account as `CUSTOMER`. User Service publishes `user-registered-topic`; Auth Service consumes it before login can succeed.

**Expected:** HTTP `201 Created`, with JSON containing `id`, `email`, `userRole`, and a message. Save `id` for the admin role-assignment API.

If the same username is registered again, expect a conflict/error (commonly `409`) rather than a second account. If the actual response differs, inspect the exception mapping in Auth Service.

### API 2 — Login customer and obtain JWT

**Request**

```http
POST http://localhost:8080/api/v1/auth/token
Content-Type: application/json
```

```json
{
  "username": "customer1@example.com",
  "password": "Customer-Pass-123!"
}
```

**Expected:** HTTP `200 OK` and a raw JWT string (not a JSON object). Copy the complete token without quotes/spaces into Postman's Bearer Token field. A failed username/password should not return a valid token.

### API 3 — Validate token (direct Auth Service endpoint)

The Gateway's `/api/v1/auth/**` route is public; the validation endpoint validates the token passed as a query parameter.

```http
GET http://localhost:8080/api/v1/auth/validate?token=<URL-ENCODED-JWT>
```

**Expected body:**

```text
Token is valid
```

URL-encode the JWT if Postman or the browser changes characters in the query string. Do not share real tokens in screenshots or GitHub.

### API 4 — Admin assigns restaurant-owner role

First log in with the bootstrapped admin account using API 2 and save `ADMIN_JWT`. Register another account with API 1, for example `owner1@example.com`. Obtain that account's ID from the Auth Service MongoDB collection or the returned registration message. Then call:

```http
PATCH http://localhost:8080/api/v1/user/<OWNER_USER_ID>/role?role=RESTAURANT_OWNER
Authorization: Bearer <ADMIN_JWT>
```

**Expected:** HTTP `200 OK`, with a user DTO showing the target account and `userRole` equal to `RESTAURANT_OWNER`. The exact fields depend on the DTO. A customer token should receive `403 Forbidden`; an unsupported role should receive `400 Bad Request`.

Repeat with `role=DELIVERY_AGENT` for the delivery account. The endpoint is intentionally limited to assigning `RESTAURANT_OWNER` or `DELIVERY_AGENT`; public registration cannot self-assign privileged roles.

### API 5 — Login owner again

```http
POST http://localhost:8080/api/v1/auth/token
Content-Type: application/json
```

```json
{
  "username": "owner1@example.com",
  "password": "Owner-Pass-123!"
}
```

**Expected:** HTTP `200 OK` and a new raw JWT. Use this new token for restaurant/food-item writes. An older token may still carry the old `CUSTOMER` role until it expires.

### API 6 — Create restaurant (owner/admin only)

```http
POST http://localhost:8080/api/v1/restaurant
Authorization: Bearer <OWNER_JWT>
Content-Type: application/json
```

```json
{
  "name": "Demo Kitchen",
  "description": "Test restaurant for API verification",
  "address": {
    "address": "24 Market Road",
    "city": "Pune",
    "state": "Maharashtra",
    "zipcode": 411001
  },
  "contactInfo": [9876543210],
  "rating": 4.2
}
```

**Expected:** HTTP `201 Created` and a success string from the service. Save the generated restaurant ID from MongoDB/service output or the returned message; use it in the food-item request. A customer token should receive `403 Forbidden`.

### API 7 — List restaurants

```http
GET http://localhost:8080/api/v1/restaurant
Authorization: Bearer <CUSTOMER_JWT>
```

**Expected:** HTTP `200 OK` and a JSON array. It may be empty (`[]`) before any restaurant is created. Example shape:

```json
[
  {
    "id": "<restaurant-id>",
    "name": "Demo Kitchen",
    "description": "Test restaurant for API verification",
    "rating": 4.2
  }
]
```

The actual DTO includes additional fields such as address/contact information and may include `foodItems`.

### API 8 — Search restaurants (pagination)

```http
GET http://localhost:8080/api/v1/restaurant/search?page=0&size=10&sortBy=name&direction=asc&name=Demo&minRating=4
Authorization: Bearer <CUSTOMER_JWT>
```

**Expected:** HTTP `200 OK` with a Spring `Page` response, typically containing `content`, `totalElements`, `totalPages`, `number`, and paging metadata. The content depends on saved restaurant data and filters.

### API 9 — Get restaurant by ID

```http
GET http://localhost:8080/api/v1/restaurant/<RESTAURANT_ID>
Authorization: Bearer <CUSTOMER_JWT>
```

**Expected:** HTTP `200 OK` and a response wrapper with restaurant details, typically `restaurantDto`, `responseCode`, and `msg`. A nonexistent ID should return the service's not-found error.

### API 10 — Add food item (owner/admin only)

```http
POST http://localhost:8080/api/v1/fooditem
Authorization: Bearer <OWNER_JWT>
Content-Type: application/json
```

```json
{
  "name": "Paneer Wrap",
  "description": "Paneer wrap for API testing",
  "price": 180.00,
  "quantity": 20,
  "restaurantId": "<RESTAURANT_ID>"
}
```

**Expected:** HTTP `201 Created` and a success string. Save the generated food-item ID from the service response/log/database; the order request needs it. A customer token should receive `403 Forbidden`.

### API 11 — List food items for a restaurant

```http
GET http://localhost:8080/api/v1/fooditem/<RESTAURANT_ID>
Authorization: Bearer <CUSTOMER_JWT>
```

**Expected:** HTTP `200 OK` and a JSON array of food items. Example shape:

```json
[
  {
    "id": "<food-item-id>",
    "name": "Paneer Wrap",
    "description": "Paneer wrap for API testing",
    "price": 180.00,
    "quantity": 20,
    "restaurantId": "<RESTAURANT_ID>"
  }
]
```

### API 12 — Place order (customer/admin only)

```http
POST http://localhost:8080/api/v1/order
Authorization: Bearer <CUSTOMER_JWT>
Content-Type: application/json
```

```json
{
  "restaurantId": "<RESTAURANT_ID>",
  "orderItems": [
    {
      "name": "Paneer Wrap",
      "price": 180,
      "quantity": 2,
      "foodItemId": "<FOOD_ITEM_ID>"
    }
  ],
  "totalAmount": 360.00,
  "address": {
    "address": "12 Sample Road",
    "city": "Pune",
    "state": "Maharashtra",
    "zipcode": 411001
  }
}
```

The service sets `userId` from the verified JWT identity rather than trusting a client-supplied `userId`. **Use real IDs and prices from your database.**

**Expected:** HTTP `201 Created` and an order response containing fields similar to:

```json
{
  "orderNumber": 10001,
  "restaurantName": "Demo Kitchen",
  "userName": "customer1@example.com",
  "orderItems": [
    { "name": "Paneer Wrap", "price": 180, "quantity": 2 }
  ],
  "totalAmount": 360.00,
  "orderStatus": "<initial-status>",
  "address": {
    "address": "12 Sample Road",
    "city": "Pune",
    "state": "Maharashtra",
    "zipcode": 411001
  }
}
```

The order number, initial status, and delivery time are generated by the implementation and can differ. Keep the `orderId` from the database/service logs for payment and status updates; the response DTO may expose an order number rather than the MongoDB ID.

### API 13 — Initiate demo payment

```http
POST http://localhost:8080/api/v1/payment
Authorization: Bearer <CUSTOMER_JWT>
Content-Type: application/json
```

```json
{
  "amount": 360.00,
  "orderId": "<ORDER_ID>",
  "creditCardInfo": {
    "cardNumber": "4111111111111111",
    "cardHolderName": "Test Customer",
    "expiryMonth": "12",
    "expiryYear": "2030",
    "securityCode": "123"
  }
}
```

Use fake test data only. Never use real card details. **This project does not integrate with a payment provider and is not safe for real payments.**

**Expected:** HTTP `201 Created` and a payment result string; current demo logic marks payment as approved. Missing order ID, null/non-positive/non-finite amount should be rejected by the service's input checks. A non-customer/non-admin token should receive `403 Forbidden`.

### API 14 — Assign delivery-agent role and login

Admin assigns the role:

```http
PATCH http://localhost:8080/api/v1/user/<AGENT_USER_ID>/role?role=DELIVERY_AGENT
Authorization: Bearer <ADMIN_JWT>
```

Then log in with that agent's username/password using API 2. Save the newly issued `AGENT_JWT`.

### API 15 — Delivery endpoint

```http
POST http://localhost:8080/api/v1/delivery/<ORDER_ID>
Authorization: Bearer <AGENT_JWT>
```

**Expected:** HTTP `201 Created` and a delivery result string if the order/delivery operation succeeds. A customer or restaurant-owner token should receive `403 Forbidden`. The exact downstream order status transition depends on the service implementation and available delivery-agent records.

### API 16 — Update order status (role-dependent)

```http
PUT http://localhost:8080/api/v1/order/status?orderId=<ORDER_ID>
Authorization: Bearer <OWNER_JWT>
Content-Type: application/json
```

Body example for a restaurant owner:

```json
"PREPARING"
```

The controller accepts an `OrderStatus` enum value as the JSON body. Expected successful response: HTTP `200 OK` with an empty body. The role/status rules in this version are:

| Status | Allowed role |
|---|---|
| `CONFIRMED`, `PREPARING` | `RESTAURANT_OWNER`, `ADMIN` |
| `OUT_FOR_DELIVERY`, `DELIVERING`, `DELIVERED` | `DELIVERY_AGENT`, `ADMIN` |
| `CANCELLED` | `CUSTOMER`, `ADMIN` |
| `COMPLETED` | `ADMIN` |

The service may have additional business constraints. Use the exact enum spelling present in `OrderStatus.java`.

## 8. Negative/security tests

Run these after the happy path:

| Test | Request | Expected result |
|---|---|---|
| No token on a protected restaurant route | `GET /api/v1/restaurant` without Bearer token | `401 Unauthorized` |
| Invalid token | Same request with a random/modified JWT | `401 Unauthorized` |
| Customer creates restaurant | `POST /api/v1/restaurant` with customer JWT | `403 Forbidden` |
| Customer adds food item | `POST /api/v1/fooditem` with customer JWT | `403 Forbidden` |
| Customer assigns privileged role | `PATCH /api/v1/user/<id>/role?role=ADMIN` with customer JWT | `403 Forbidden` |
| Admin assigns unsupported role | `PATCH ...?role=ADMIN` with admin JWT | `400 Bad Request` (assignment endpoint intentionally only accepts owner/agent roles) |
| Wrong password | `POST /api/v1/auth/token` with wrong password | Authentication failure; no JWT |
| Duplicate registration | Register the same username twice | Error/conflict; no duplicate account |
| Customer updates order to `PREPARING` | Status endpoint with customer JWT | `403 Forbidden` |
| Restaurant owner initiates payment | Payment endpoint with owner JWT | `403 Forbidden` |

Error JSON formatting may vary because global exception handling differs by service. Judge the HTTP status and error meaning, not an exact error-body string.

## 9. Service routes and important architecture notes

Gateway routes configured in this project:

- `/api/v1/auth/**` → Auth Service (public gateway filter is disabled for this route; Auth Service still authenticates the login endpoint itself)
- `/api/v1/user/**` → Auth Service
- `/api/v1/restaurant/**` → Restaurant Service
- `/api/v1/fooditem/**` → Restaurant Service
- `/api/v1/order/**` → Order Service
- `/api/v1/payment/**` → Payment Service
- `/api/v1/delivery/**` → Delivery Service

The standalone User Service is not the target of the Gateway's `/api/v1/user/**` route. Auth Service is the source of roles used to issue JWTs. User Service currently uses its own database; records are **not automatically synchronized** between these services.

## 10. Common problems and fixes

**401 on a valid-looking token**
- Confirm the `Authorization` header is `Bearer <token>`.
- Ensure Auth Service, Gateway and User Service use the exact same `JWT_SECRET_BASE64` value.
- Log in again after changing roles; old JWTs contain old role claims.

**403 on owner/admin API**
- Verify that the account's role is correct in the Auth Service database.
- Log in again and use the new JWT.
- Don't try to spoof `loggedInRole`; Gateway strips client-supplied identity headers.

**Service does not appear in Eureka**
- Start Discovery Server first.
- Check the `EUREKA_SERVER_URL` value and the service startup log.
- Wait for registration and refresh `http://localhost:8761`.

**MongoDB connection error**
- Start MongoDB and check `MONGODB_HOST`, `MONGODB_PORT`, and the service-specific database setting.

**Kafka connection error or missing event notification**
- Start Kafka at `localhost:9092` and check producer/consumer logs and topic configuration.

**Maven build error**
- Run `mvn clean package -DskipTests` from the root directory.
- Share the first actual compiler error and the file/line if the build fails; later errors may be consequences of the first.

**Random ports confuse direct API testing**
- Use Gateway port `8080` for routes listed above. `server.port=0` means the direct service port is randomly allocated.

## 11. Production gaps — do not deploy this demo as-is

- Payment is simulated; integrate a provider, verify signed webhooks, and implement idempotency/reconciliation before real payments.
- Protect Eureka and internal service endpoints in production; this local configuration is for development.
- Restrict food-item updates to the owner of the specific restaurant, not merely any restaurant-owner role.
- Add robust service-to-service authentication and network policies; forwarded identity headers alone are not a complete production trust boundary.
- Add centralized exception handling, validation tests, integration tests, rate limiting, secrets management, monitoring, and token expiry/refresh strategy.
- Never commit secrets, passwords, JWTs, or real card data to Git.

## 12. Suggested Postman collection order

Create a Postman collection called **Food Delivery Microservices — Local** and use these variables:

```text
baseUrl = http://localhost:8080
customerToken = <JWT>
adminToken = <JWT>
ownerToken = <JWT>
agentToken = <JWT>
restaurantId = <MongoDB restaurant ID>
foodItemId = <MongoDB food-item ID>
orderId = <MongoDB order ID>
```

Create requests in this order: Register Customer → Login Customer → Login Admin → Register Owner → Admin Assign Owner Role → Login Owner → Create Restaurant → List Restaurants → Add Food Item → List Food Items → Place Order → Pay (Demo) → Register Agent → Admin Assign Agent Role → Login Agent → Deliver Order → Negative Security Tests.

---

**Local testing reminder:** use the JSON examples as templates, replace every `<...>` placeholder with real values, and inspect the controller/service code if your actual status code or response shape differs. The project is a learning/demo system, not a production-ready food delivery platform.

## API testing assets added

See [`README-API-TESTING.md`](README-API-TESTING.md) for the import/run guide, [`API-AUDIT.md`](API-AUDIT.md) for source-review findings, and the `postman/` folder for the Postman collection and environment. Runtime behavior must be verified against your local services; this archive has not been integration-tested in this environment.

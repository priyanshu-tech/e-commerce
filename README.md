# E-Commerce Backend API - Complete coding using AI Tool

A production-grade e-commerce backend built with **Spring Boot** and **Oracle DB**, implementing real-world patterns across user management, product catalog, inventory lifecycle, and shopping cart.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3 |
| ORM | Spring Data JPA (Hibernate) |
| Database | Oracle DB |
| Config Management | Spring Cloud Config (`@RefreshScope`) |
| Scheduling | Spring `@Scheduled` |
| Build Tool | Maven |
| API Testing | Postman |

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────┐
│                   REST Controllers                   │
│   UserController  ProductController  CartController  │
│   InventoryController  (OrderController - WIP)       │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                   Service Layer                      │
│   UserService  ProductService  CartService           │
│   InventoryService                                   │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│               Repository Layer (JPA)                 │
│   UserRepo  ProductRepo  CartRepo  InventoryRepo     │
└────────────────────────┬────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────┐
│                    Oracle DB                         │
│   USERS  ADDRESSES  PRODUCTS  CATEGORIES             │
│   PRODUCT_IMAGES  DISCOUNTS  INVENTORY               │
│   INVENTORY_RESERVATIONS  CARTS  CART_ITEMS          │
└─────────────────────────────────────────────────────┘
```

---

## Modules Implemented

### User API `/api/users`
- Create, fetch, update user profile
- Surrogate `userId` PK via `USERS_SEQ` — `username` and `email` individually unique
- Add and fetch saved addresses per user
- Single default address per user — auto-unset on new default
- [Full Docs](docs/USER_API.md)

### Product API `/api/products`
- Paginated product listing with category filter and keyword search
- Dynamic discount — computed at query time from `DISCOUNTS` table, capped by `maxDiscountPct` via Spring Cloud Config
- Product images in separate `PRODUCT_IMAGES` table
- Toggle status (`ACTIVE` ↔ `INACTIVE`) instead of hard delete
- Separate `updateRating` endpoint for Reviews service integration
- [Full Docs](docs/PRODUCT_API.md)

### Inventory API `/api/inventory`
- Full stock lifecycle — create → restock → reserve → confirm/release
- No reservation at cart-add — only at checkout
- 15-minute time-boxed reservations with `@Scheduled` auto-release every 60 seconds
- `isLowStock` computed at query time (`availableQuantity <= minStockLevel`)
- Inventory created via SKU lookup — `productId` resolved internally
- [Full Docs](docs/INVENTORY_API.md)

### Cart API `/api/cart`
- Cart persisted in DB — survives browser close
- Auto-created on first access — no manual cart creation
- Duplicate product add increments quantity, no duplicate row
- Stock + status check at cart-add (`ACTIVE` + `availableQuantity > 0`)
- `unitPrice` reflects active discount if exists, else original price
- `subtotal` = sum of discounted prices × quantities
- `totalItems` = sum of all quantities
- [Full Docs](docs/CART_API.md)

---

## Key Design Patterns

- **Surrogate PKs via Oracle Sequences** — all tables use `GenerationType.SEQUENCE` with `allocationSize=1` matching `INCREMENT BY 1` in DDL
- **Single logging point** — only `GlobalExceptionHandler` logs errors, no try-catch in service/mapper layers
- **Mapper pattern** — dedicated static mapper classes per domain (`UserMapper`, `ProductMapper`, `InventoryMapper`, `CartMapper`)
- **VO pattern** — all API responses use Value Objects, entities never exposed directly
- **`@RefreshScope`** — `DiscountConfig` refreshable at runtime via `POST /actuator/refresh` without restart
- **Soft delete via toggle** — products are never hard deleted, status toggled between `ACTIVE`/`INACTIVE`
- **Address snapshot on order** — full address copied at order time, not just FK reference

---

## Database Schema

| File | Description |
|---|---|
| [`docs/user-schema.sql`](docs/user-schema.sql) | `USERS`, `ADDRESSES` tables + sample data |
| [`docs/product-schema.sql`](docs/product-schema.sql) | `PRODUCTS`, `CATEGORIES`, `PRODUCT_IMAGES`, `DISCOUNTS` + sample data |
| [`docs/inventory-schema.sql`](docs/inventory-schema.sql) | `INVENTORY`, `INVENTORY_RESERVATIONS` + sample data |
| [`docs/cart-schema.sql`](docs/cart-schema.sql) | `CARTS`, `CART_ITEMS` + sample data |

---

## API Documentation

| Module | Docs |
|---|---|
| User API | [USER_API.md](docs/USER_API.md) |
| Product API | [PRODUCT_API.md](docs/PRODUCT_API.md) |
| Inventory API | [INVENTORY_API.md](docs/INVENTORY_API.md) |
| Cart API | [CART_API.md](docs/CART_API.md) |

---

## Running Locally

### Prerequisites
- Java 17+
- Maven 3.8+
- Oracle DB (running locally or via Docker)

### Steps

```bash
# Clone the repo
git clone https://github.com/<your-username>/<repo-name>.git
cd <repo-name>

# Configure DB credentials
# Edit src/main/resources/application.yaml (not committed — add to .gitignore)

# Build and run
mvn spring-boot:run
```

### application.yaml (not committed)

```yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:xe
    username: <your-db-username>
    password: <your-db-password>
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true

server:
  port: 8097

discount:
  max-discount-pct: 90.00
```

---

## Postman Collections

| Module | Collection |
|---|---|
| Cart API | [cart-postman-collection.json](docs/cart-postman-collection.json) |
| Inventory API | [inventory-postman-collection.json](docs/inventory-postman-collection.json) |

---

## Upcoming

- Order API — place order from cart, cancel order, order history
- Payment API — Razorpay test mode integration
- Reviews API — add review, update product rating
- JUnit unit tests
- Integration tests (Spring Boot Test + Oracle)
- Performance tests (JMeter)

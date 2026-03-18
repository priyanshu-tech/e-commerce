# Order API Documentation

## Overview

Manages order placement and lifecycle. Orders are placed from the active cart — no manual item selection needed. Inventory is reserved per item at order time. Cart is cleared automatically after a successful order. Only `CONFIRMED` orders can be cancelled.

---

## Base URL

```
http://localhost:8097/api/orders
```

---

## Data Models

### Order

| Field                   | Type          | Notes                                              |
|-------------------------|---------------|----------------------------------------------------|
| `orderId`               | Long          | Surrogate PK, auto-generated via `ORDERS_SEQ`     |
| `orderNumber`           | String        | Human-readable ID — `ORD-YYYYMMDD-{orderId}`      |
| `userId`                | Long          | FK → USERS                                        |
| `totalAmount`           | BigDecimal    | Sum of `finalPrice` across all items              |
| `orderStatus`           | String        | `CONFIRMED` or `CANCELLED`                        |
| `orderDate`             | LocalDateTime | Set on insert, never updated                      |
| `shippingAddressLine1`  | String        | Address snapshot — copied at order time           |
| `shippingAddressLine2`  | String        | Optional                                          |
| `shippingCity`          | String        | Address snapshot                                  |
| `shippingState`         | String        | Address snapshot                                  |
| `shippingZipCode`       | String        | Address snapshot                                  |
| `shippingCountry`       | String        | Address snapshot                                  |

### OrderItem

| Field            | Type       | Notes                                                  |
|------------------|------------|--------------------------------------------------------|
| `orderItemId`    | Long       | Surrogate PK, auto-generated via `ORDER_ITEMS_SEQ`    |
| `orderId`        | Long       | FK → ORDERS                                           |
| `productId`      | Long       | FK → PRODUCTS                                         |
| `productName`    | String     | Product name snapshot at order time                   |
| `sku`            | String     | Product SKU snapshot at order time                    |
| `quantity`       | Integer    | Units ordered                                         |
| `originalPrice`  | BigDecimal | Product price before discount                         |
| `discountedPrice`| BigDecimal | Price after discount cap applied                      |
| `finalPrice`     | BigDecimal | `discountedPrice × quantity`                          |

### OrderVO (Response)

| Field                   | Type               | Notes                                         |
|-------------------------|--------------------|-----------------------------------------------|
| `orderId`               | Long               | Order identifier                              |
| `orderNumber`           | String             | Human-readable order number                   |
| `userId`                | Long               | Owner of the order                            |
| `items`                 | List\<OrderItemVO\> | All items in the order                        |
| `totalAmount`           | BigDecimal         | Total order value                             |
| `orderStatus`           | String             | `CONFIRMED` or `CANCELLED`                    |
| `orderDate`             | LocalDateTime      | When the order was placed                     |
| `shippingAddressLine1`  | String             | Snapshot of shipping address                  |
| `shippingAddressLine2`  | String             | Optional                                      |
| `shippingCity`          | String             |                                               |
| `shippingState`         | String             |                                               |
| `shippingZipCode`       | String             |                                               |
| `shippingCountry`       | String             |                                               |

### OrderItemVO (Response)

| Field             | Type       | Notes                                      |
|-------------------|------------|--------------------------------------------|
| `orderItemId`     | Long       | Order item identifier                      |
| `productId`       | Long       | Product identifier                         |
| `productName`     | String     | Product name at time of order              |
| `sku`             | String     | SKU at time of order                       |
| `quantity`        | Integer    | Units ordered                              |
| `originalPrice`   | BigDecimal | Price before discount                      |
| `discountedPrice` | BigDecimal | Price after discount                       |
| `finalPrice`      | BigDecimal | `discountedPrice × quantity`               |

---

## Endpoints

### 1. Place Order

Places an order from the user's active cart. Cart must not be empty. Inventory is reserved per item. Cart is cleared on success.

```
POST /api/orders?userId={userId}&addressId={addressId}
```

**Query Params**

| Param       | Required | Description                        |
|-------------|----------|------------------------------------|
| `userId`    | Yes      | User placing the order             |
| `addressId` | Yes      | Saved address to ship to           |

**Success Response — 201 Created**

```json
{
  "orderId": 1,
  "orderNumber": "ORD-20250101-0001",
  "userId": 1,
  "items": [
    {
      "orderItemId": 1,
      "productId": 1,
      "productName": "Wireless Headphones",
      "sku": "WH-1000XM5",
      "quantity": 1,
      "originalPrice": 999.99,
      "discountedPrice": 849.99,
      "finalPrice": 849.99
    },
    {
      "orderItemId": 2,
      "productId": 2,
      "productName": "USB-C Cable",
      "sku": "USB-C-2M",
      "quantity": 2,
      "originalPrice": 29.99,
      "discountedPrice": 24.99,
      "finalPrice": 49.98
    }
  ],
  "totalAmount": 899.97,
  "orderStatus": "CONFIRMED",
  "orderDate": "2025-01-01T10:30:00",
  "shippingAddressLine1": "123 Main Street",
  "shippingAddressLine2": null,
  "shippingCity": "Mumbai",
  "shippingState": "Maharashtra",
  "shippingZipCode": "400001",
  "shippingCountry": "India"
}
```

**Error Responses**

| Status | Scenario                                      |
|--------|-----------------------------------------------|
| 404    | Cart not found for userId                     |
| 404    | Address not found for addressId               |
| 404    | Product not found for a cart item             |
| 404    | Inventory not found for a product             |
| 500    | Cart is empty                                 |
| 500    | Product is INACTIVE                           |
| 500    | Insufficient stock for a product              |

---

### 2. Get Order by Order Number

Fetches a single order with all its items.

```
GET /api/orders/{orderNumber}
```

**Path Variable**

| Variable      | Description                          |
|---------------|--------------------------------------|
| `orderNumber` | e.g. `ORD-20250101-0001`             |

**Success Response — 200 OK** — returns OrderVO (same shape as above)

**Error Responses**

| Status | Scenario              |
|--------|-----------------------|
| 404    | Order not found       |

---

### 3. Get Orders by User

Returns paginated list of orders for a user, sorted by `orderDate` descending.

```
GET /api/orders/user/{userId}?page=0&size=10
```

**Path Variable**

| Variable | Description     |
|----------|-----------------|
| `userId` | User identifier |

**Query Params**

| Param  | Default | Description        |
|--------|---------|--------------------|
| `page` | 0       | Page number        |
| `size` | 10      | Results per page   |

**Success Response — 200 OK**

```json
[
  {
    "orderId": 2,
    "orderNumber": "ORD-20250102-0002",
    ...
  },
  {
    "orderId": 1,
    "orderNumber": "ORD-20250101-0001",
    ...
  }
]
```

---

### 4. Cancel Order

Cancels a `CONFIRMED` order. Releases all inventory reservations. Only `CONFIRMED` orders can be cancelled.

```
POST /api/orders/{orderNumber}/cancel
```

**Path Variable**

| Variable      | Description                          |
|---------------|--------------------------------------|
| `orderNumber` | e.g. `ORD-20250101-0001`             |

**Success Response — 200 OK**

```json
{
  "status": "success",
  "message": "Order ORD-20250101-0001 cancelled successfully"
}
```

**Error Responses**

| Status | Scenario                                          |
|--------|---------------------------------------------------|
| 404    | Order not found                                   |
| 500    | Order is not in CONFIRMED status                  |

---

## Error Response Shape

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Order not found: ORD-20250101-0099",
  "path": "/api/orders/ORD-20250101-0099",
  "timestamp": "2025-01-01T10:30:00"
}
```

---

## Key Design Decisions

- **Place order from cart**: No item selection in request — entire cart is ordered. Only `userId` + `addressId` needed.
- **Address snapshot**: Full address fields copied into `ORDERS` at order time — not a FK reference. Ensures order history is preserved even if user updates or deletes the address later.
- **Price snapshot**: `originalPrice`, `discountedPrice`, `finalPrice` captured per item at order time — not recomputed on fetch. Ensures order history reflects prices at time of purchase.
- **orderNumber format**: `ORD-YYYYMMDD-{orderId}` — e.g. `ORD-20250101-0001`. Uses the sequence-generated `orderId` padded to 4 digits.
- **Inventory reserved at order time**: `availableQuantity` decremented, `reservedQuantity` incremented, and an `INVENTORY_RESERVATIONS` row created per item with a 15-minute expiry.
- **Cart cleared on success**: After order is saved and inventory reserved, cart items are deleted automatically.
- **Cancel only CONFIRMED**: Attempting to cancel an already `CANCELLED` order throws an error. Cancel releases all reservations and restores `availableQuantity`.

# Cart API Documentation

## Overview

Manages the shopping cart for a user. Cart is persisted in DB — users can close the browser and return to find their cart intact. A cart is auto-created on first access. No stock is reserved at cart-add — reservation only happens at checkout via the Inventory API.

---

## Base URL

```
http://localhost:8097/api/cart
```

---

## Data Models

### Cart

| Field       | Type          | Notes                                          |
|-------------|---------------|------------------------------------------------|
| `cartId`    | Long          | Surrogate PK, auto-generated via `CARTS_SEQ`  |
| `userId`    | Long          | FK → USERS, unique — one cart per user        |
| `createdAt` | LocalDateTime | Set on insert, never updated                  |

### CartItem

| Field         | Type          | Notes                                               |
|---------------|---------------|-----------------------------------------------------|
| `cartItemId`  | Long          | Surrogate PK, auto-generated via `CART_ITEMS_SEQ`  |
| `cartId`      | Long          | FK → CARTS                                         |
| `productId`   | Long          | FK → PRODUCTS                                      |
| `quantity`    | Integer       | Total units of this product in cart                |
| `addedAt`     | LocalDateTime | Set on insert, never updated                       |

### CartVO (Response)

| Field        | Type              | Notes                                                              |
|--------------|-------------------|--------------------------------------------------------------------|
| `cartId`     | Long              | Cart identifier                                                    |
| `userId`     | Long              | Owner of the cart                                                  |
| `items`      | List\<CartItemVO\> | All items in cart                                                  |
| `subtotal`   | BigDecimal        | Sum of `unitPrice × quantity` across all items (discounted price) |
| `totalItems` | Integer           | Sum of all quantities across all items                            |

### CartItemVO (Response)

| Field         | Type       | Notes                                                        |
|---------------|------------|--------------------------------------------------------------|
| `cartItemId`  | Long       | Cart item identifier                                         |
| `productId`   | Long       | Product identifier                                           |
| `productName` | String     | Product name snapshot                                        |
| `sku`         | String     | Product SKU                                                  |
| `quantity`    | Integer    | Units in cart                                                |
| `unitPrice`   | BigDecimal | Discounted price if active discount exists, else original    |
| `totalPrice`  | BigDecimal | `unitPrice × quantity`                                       |

---

## Endpoints

### 1. Get Cart

Fetches the cart and all items for a user. Auto-creates an empty cart if none exists.

```
GET /api/cart/{userId}
```

**Success Response — 200 OK**

```json
{
  "cartId": 1,
  "userId": 1,
  "items": [
    {
      "cartItemId": 1,
      "productId": 1,
      "productName": "Samsung Galaxy S24",
      "sku": "SAMSUNG-S24-BLK-128",
      "quantity": 2,
      "unitPrice": 59999.00,
      "totalPrice": 119998.00
    },
    {
      "cartItemId": 2,
      "productId": 2,
      "productName": "Apple iPhone 15",
      "sku": "APPLE-IP15-BLK-128",
      "quantity": 1,
      "unitPrice": 79999.00,
      "totalPrice": 79999.00
    }
  ],
  "subtotal": 199997.00,
  "totalItems": 3
}
```

---

### 2. Add Item to Cart

Adds a product to the cart. If the product already exists in the cart, quantity is incremented — no duplicate row created.

```
POST /api/cart/{userId}/items
```

**Request Body**

```json
{
  "productId": 1,
  "quantity": 2
}
```

**Success Response — 201 Created** — returns updated CartVO

**Error Responses**

| Status | Scenario                          |
|--------|-----------------------------------|
| 404    | Product not found                 |
| 500    | Product is INACTIVE               |
| 500    | Product is out of stock           |

---

### 3. Update Cart Item Quantity

Updates the quantity of an existing cart item.

```
POST /api/cart/{userId}/items/{cartItemId}?quantity={quantity}
```

**Query Params**

| Param      | Required | Description       |
|------------|----------|-------------------|
| `quantity` | Yes      | New quantity value |

**Success Response — 200 OK** — returns updated CartVO

**Error Responses**

| Status | Scenario             |
|--------|----------------------|
| 404    | Cart item not found  |

---

### 4. Remove Item from Cart

Removes a single item from the cart by `cartItemId`.

```
POST /api/cart/{userId}/items/{cartItemId}/remove
```

**Success Response — 200 OK** — returns updated CartVO

**Error Responses**

| Status | Scenario             |
|--------|----------------------|
| 404    | Cart item not found  |

---

### 5. Clear Cart

Removes all items from the cart. Called internally by Order service after order is placed. Also exposed for manual clear by user.

```
POST /api/cart/{userId}/clear
```

**Success Response — 200 OK**

```json
{
  "status": "success",
  "message": "Cart cleared successfully"
}
```

---

## Error Response Shape

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Cart item not found with id: 99",
  "path": "/api/cart/1/items/99/remove",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Key Design Decisions

- **Auto-create cart**: No separate endpoint to create a cart. `getCart` and `addItemToCart` both auto-create a cart if none exists for the user.
- **No reservation at cart-add**: Stock is only checked for availability (`availableQuantity > 0`) — not reserved. Reservation happens at checkout via `POST /api/inventory/{inventoryId}/reserve`.
- **Duplicate product handling**: Adding the same product twice increments quantity — no duplicate `CART_ITEMS` row. Enforced by `UNIQUE (CART_ID, PRODUCT_ID)` constraint at DB level.
- **Discounted unit price**: `unitPrice` in response reflects active discount if one exists, capped by `maxDiscountPct` from Spring Cloud Config. Falls back to original price if no active discount.
- **totalItems is total quantity**: Sum of all quantities across all items, not count of distinct products.
- **clearCart called by Order service**: After order is placed successfully, Order service calls `clearCart` internally to empty the cart.

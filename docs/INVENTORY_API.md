# Inventory API Documentation

## Overview

Manages product stock levels across the order lifecycle. Inventory is never reserved at cart-add — reservation only happens at checkout (proceed to payment). A scheduled job runs every 60 seconds to auto-release expired reservations, preventing stock from being locked indefinitely.

---

## Base URL

```
http://localhost:8097/api/inventory
```

---

## Stock Lifecycle

```
Product Created → createInventory (stock = 0)
                       ↓
Admin Restocks  → restock (totalQuantity + availableQuantity ↑)
                       ↓
User Checks Out → reserve (availableQuantity ↓, reservedQuantity ↑, 15 min timer starts)
                       ↓
         ┌─────────────┴─────────────┐
    Payment Success             Payment Fails / Cancelled / Timer Expires
         ↓                                      ↓
      confirm                                release
(totalQuantity ↓,                   (availableQuantity ↑,
 reservedQuantity ↓,                 reservedQuantity ↓,
 reservation deleted)                reservation deleted)
```

---

## Data Models

### Inventory

| Field               | Type          | Notes                                                              |
|---------------------|---------------|--------------------------------------------------------------------|
| `inventoryId`       | Long          | Surrogate PK, auto-generated via `INVENTORY_SEQ`                  |
| `productId`         | Long          | FK → PRODUCTS, unique — one inventory record per product          |
| `sku`               | String        | Unique, mirrors `PRODUCTS.SKU` — primary lookup key               |
| `totalQuantity`     | Integer       | Total physical stock in warehouse                                  |
| `availableQuantity` | Integer       | Stock available for purchase. Shown on product page               |
| `reservedQuantity`  | Integer       | Stock held during active checkout sessions                        |
| `minStockLevel`     | Integer       | Threshold for low stock warning. Defaults to `10`                 |
| `isLowStock`        | Boolean       | `true` if `availableQuantity <= minStockLevel`. Computed, not stored |
| `warehouseLocation` | String        | Physical shelf location e.g. `WH-DELHI-A1`                       |
| `lastUpdated`       | LocalDateTime | Auto-updated on every change                                      |

### InventoryReservation

| Field           | Type          | Notes                                                    |
|-----------------|---------------|----------------------------------------------------------|
| `reservationId` | Long          | Surrogate PK, auto-generated via `INVENTORY_RESERVATIONS_SEQ` |
| `inventoryId`   | Long          | FK → INVENTORY                                           |
| `orderId`       | Long          | Which checkout session holds this reservation            |
| `quantity`      | Integer       | How much stock is held                                   |
| `expiresAt`     | LocalDateTime | `createdAt + 15 minutes` — auto-released after this     |
| `createdAt`     | LocalDateTime | Set on insert                                            |

---

## Endpoints

### 1. Get Inventory by ProductId

```
GET /api/inventory/product/{productId}
```

**Success Response — 200 OK**

```json
{
  "inventoryId": 1,
  "productId": 1,
  "sku": "SAMSUNG-S24-BLK-128",
  "totalQuantity": 150,
  "availableQuantity": 148,
  "reservedQuantity": 2,
  "minStockLevel": 10,
  "isLowStock": false,
  "warehouseLocation": "WH-DELHI-A1",
  "lastUpdated": "2024-01-15T10:30:00"
}
```

**Error Responses**

| Status | Scenario                          |
|--------|-----------------------------------|
| 404    | No inventory record for productId |

---

### 2. Get Inventory by SKU

```
GET /api/inventory/sku/{sku}
```

**Success Response — 200 OK** — same shape as Get by ProductId

**Error Responses**

| Status | Scenario                    |
|--------|-----------------------------|
| 404    | No inventory record for SKU |

---

### 3. Create Inventory

Creates an inventory record for a product. Called by admin after a product is created. `sku` is used to look up the product — `productId` is resolved internally. `availableQuantity` is set equal to `totalQuantity` on create. `reservedQuantity` starts at `0`.

```
POST /api/inventory
```

**Request Body**

```json
{
  "sku": "SONY-WH1000XM5-BLK",
  "totalQuantity": 50,
  "minStockLevel": 5,
  "warehouseLocation": "WH-DELHI-A1"
}
```

**Success Response — 201 Created** — returns saved InventoryVO with `productId` populated

**Error Responses**

| Status | Scenario                                  |
|--------|-------------------------------------------|
| 404    | No product found with given SKU           |
| 409    | Inventory already exists for this product |

---

### 4. Restock

Adds stock to an existing inventory record. Admin operation — called when warehouse receives a new shipment.

```
POST /api/inventory/{inventoryId}/restock?quantity={quantity}
```

**Query Params**

| Param      | Required | Description              |
|------------|----------|--------------------------|
| `quantity` | Yes      | Units to add to stock    |

**Success Response — 200 OK** — returns updated InventoryVO with incremented `totalQuantity` and `availableQuantity`

**Error Responses**

| Status | Scenario              |
|--------|-----------------------|
| 404    | Inventory not found   |

---

### 5. Reserve (Checkout)

Holds stock for a checkout session for 15 minutes. Called by Order service when user proceeds to payment. Decrements `availableQuantity`, increments `reservedQuantity`.

```
POST /api/inventory/{inventoryId}/reserve?orderId={orderId}&quantity={quantity}
```

**Query Params**

| Param      | Required | Description                    |
|------------|----------|--------------------------------|
| `orderId`  | Yes      | The checkout order session ID  |
| `quantity` | Yes      | Units to reserve               |

**Success Response — 200 OK** — returns InventoryReservationVO with `expiresAt`

**Error Responses**

| Status | Scenario                          |
|--------|-----------------------------------|
| 404    | Inventory not found               |
| 500    | Insufficient stock available      |

---

### 6. Release (Order Cancelled / Timeout)

Releases a reservation back to available stock. Called when order is cancelled, payment fails, or reservation timer expires. Increments `availableQuantity`, decrements `reservedQuantity`.

```
POST /api/inventory/release?orderId={orderId}&inventoryId={inventoryId}
```

**Success Response — 200 OK** — returns updated InventoryVO

**Error Responses**

| Status | Scenario                    |
|--------|-----------------------------|
| 404    | Reservation not found       |

---

### 7. Confirm (Payment Success)

Permanently deducts stock after successful payment. Decrements both `totalQuantity` and `reservedQuantity`. Deletes the reservation record.

```
POST /api/inventory/confirm?orderId={orderId}&inventoryId={inventoryId}
```

**Success Response — 200 OK** — returns updated InventoryVO

**Error Responses**

| Status | Scenario              |
|--------|-----------------------|
| 404    | Reservation not found |

---

## Auto-Release Scheduler

A `@Scheduled` job runs every **60 seconds** and finds all reservations where `expiresAt < NOW()`. For each expired reservation:
- `availableQuantity` is incremented back
- `reservedQuantity` is decremented
- Reservation record is deleted

This prevents stock from being locked indefinitely if a user abandons checkout without cancelling.

---

## Error Response Shape

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Inventory not found for productId: 99",
  "path": "/api/inventory/product/99",
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Key Design Decisions

- **No reservation at cart-add**: Cart is just intent. Stock is only held when user actively proceeds to checkout — prevents long-term stock locking.
- **15-minute reservation window**: Gives user enough time to complete payment. Auto-released by scheduler if abandoned.
- **Three-state lifecycle**: `reserve` → `confirm` (payment success) or `release` (cancelled/expired). Each state transition is atomic within a transaction.
- **`isLowStock` computed field**: Not stored in DB — calculated at query time as `availableQuantity <= minStockLevel`. Avoids stale flag issues.
- **One inventory record per product**: `productId` and `sku` are both unique in `INVENTORY` table — enforced at DB level.

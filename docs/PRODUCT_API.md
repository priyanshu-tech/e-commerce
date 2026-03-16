# Product API Documentation

## Overview

Manages the product catalog including categories, product listings, images, and dynamic discounts. Products use a numeric surrogate PK (`productId`) auto-generated via Oracle sequence. Deletion is soft — products are marked `INACTIVE` and hidden from all listings without removing the row.

---

## Base URL

```
http://localhost:8097/api/products
```

---

## Data Models

### Product

| Field          | Type              | Notes                                                        |
|----------------|-------------------|--------------------------------------------------------------|
| `productId`    | Long              | Surrogate PK, auto-generated via `PRODUCTS_SEQ`             |
| `name`         | String            | Required                                                     |
| `description`  | String            | Optional                                                     |
| `sku`          | String            | Required, unique. Format: `BRAND-MODEL-COLOR-SIZE`          |
| `price`        | BigDecimal        | Required, base price                                         |
| `discountPrice`| BigDecimal        | Calculated at query time from active `DISCOUNTS` row. Read-only |
| `brand`        | String            | Required                                                     |
| `categoryId`   | Long              | FK → CATEGORIES                                              |
| `categoryName` | String            | Resolved from `categoryId` at query time. Read-only         |
| `images`       | List<ProductImageVO> | Stored in `PRODUCT_IMAGES` table, joined by `productId`  |
| `rating`       | Double            | Optional, updated externally                                 |
| `reviewCount`  | Integer           | Defaults to `0`                                              |
| `status`       | String            | `ACTIVE` or `INACTIVE`. Defaults to `ACTIVE`                |
| `createdAt`    | LocalDateTime     | Set on insert, never updated                                 |

### ProductImage

| Field          | Type    | Notes                                              |
|----------------|---------|----------------------------------------------------|
| `imageId`      | Long    | Surrogate PK, auto-generated via `PRODUCT_IMAGES_SEQ` |
| `productId`    | Long    | FK → PRODUCTS (join key)                           |
| `imageUrl`     | String  | Required                                           |
| `displayOrder` | Integer | Controls carousel order. Defaults to `0`           |
| `isPrimary`    | Boolean | Flags the main thumbnail. Defaults to `false`      |

### Category

| Field             | Type    | Notes                                                  |
|-------------------|---------|--------------------------------------------------------|
| `categoryId`      | Long    | Surrogate PK, auto-generated via `CATEGORIES_SEQ`     |
| `name`            | String  | Required, unique                                       |
| `description`     | String  | Optional                                               |
| `parentCategoryId`| Long    | Self-referencing FK for hierarchy. `null` = root       |
| `imageUrl`        | String  | Optional                                               |
| `displayOrder`    | Integer | Controls sort order in UI                              |

---

## Endpoints

### 1. Get All Products

Returns paginated list of `ACTIVE` products. Supports optional category filter and keyword search.

```
GET /api/products?category={categoryName}&search={keyword}&page={page}&size={size}
```

**Query Params**

| Param      | Required | Default | Description                              |
|------------|----------|---------|------------------------------------------|
| `category` | No       | —       | Filter by category name (exact match)    |
| `search`   | No       | —       | Keyword search on `name` and `description` |
| `page`     | No       | `0`     | Zero-based page number                   |
| `size`     | No       | `20`    | Page size                                |

**Success Response — 200 OK**

```json
[
  {
    "productId": 1,
    "name": "Samsung Galaxy S24",
    "description": "Latest Samsung flagship smartphone with AI features",
    "sku": "SAMSUNG-S24-BLK-128",
    "price": 79999.00,
    "discountPrice": 71999.10,
    "brand": "Samsung",
    "categoryId": 3,
    "categoryName": "Mobile Phones",
    "images": [
      {
        "imageId": 1,
        "productId": 1,
        "imageUrl": "https://images.example.com/products/s24-front.jpg",
        "displayOrder": 1,
        "isPrimary": true
      }
    ],
    "rating": 4.5,
    "reviewCount": 120,
    "status": "ACTIVE",
    "createdAt": "2024-01-15T10:30:00"
  }
]
```

**Error Responses**

| Status | Scenario                        |
|--------|---------------------------------|
| 404    | Category name not found in DB   |

---

### 2. Get Product by ID

Fetches a single `ACTIVE` product by `productId`. Returns `discountPrice` if an active discount exists.

```
GET /api/products/{productId}
```

**Success Response — 200 OK** — same shape as single item in Get All Products

**Error Responses**

| Status | Scenario                              |
|--------|---------------------------------------|
| 404    | Product not found or is `INACTIVE`    |

---

### 3. Create Product

Creates a new product. `sku` must be unique across all products. `images` are saved to `PRODUCT_IMAGES` table.

```
POST /api/products
```

**Request Body**

```json
{
  "name": "OnePlus 12",
  "description": "Flagship killer with Snapdragon 8 Gen 3",
  "sku": "ONEPLUS-12-BLK-256",
  "price": 64999.00,
  "brand": "OnePlus",
  "categoryId": 3,
  "status": "ACTIVE",
  "images": [
    {
      "imageUrl": "https://images.example.com/products/op12-front.jpg",
      "displayOrder": 1,
      "isPrimary": true
    },
    {
      "imageUrl": "https://images.example.com/products/op12-back.jpg",
      "displayOrder": 2,
      "isPrimary": false
    }
  ]
}
```

**Success Response — 201 Created** — returns saved product with generated `productId` and `imageId`s

**Error Responses**

| Status | Scenario                        |
|--------|---------------------------------|
| 409    | Product with same SKU exists    |

---

### 4. Update Product

Updates mutable fields of an existing `ACTIVE` product. `sku`, `createdAt` cannot be changed. If `images` is provided, existing images are replaced entirely.

```
POST /api/products/{productId}/update
```

**Request Body**

```json
{
  "name": "Samsung Galaxy S24 Ultra",
  "description": "Updated description with S Pen support",
  "price": 84999.00,
  "brand": "Samsung",
  "categoryId": 3,
  "images": [
    {
      "imageUrl": "https://images.example.com/products/s24ultra-front.jpg",
      "displayOrder": 1,
      "isPrimary": true
    }
  ]
}
```

> Omit `images` from the body to leave existing images unchanged.

**Success Response — 200 OK** — returns updated product

**Error Responses**

| Status | Scenario                              |
|--------|---------------------------------------|
| 404    | Product not found or is `INACTIVE`    |

---

### 5. Toggle Product Status

Toggles product visibility between `ACTIVE` and `INACTIVE`. Admin-only operation.

- `ACTIVE` → `INACTIVE`: product disappears from all listings
- `INACTIVE` → `ACTIVE`: product is relisted and visible again

```
POST /api/products/{productId}/toggle-status
```

**Success Response — 200 OK**

```json
{
  "message": "Product status changed to INACTIVE"
}
```

or

```json
{
  "message": "Product status changed to ACTIVE"
}
```

**Error Responses**

| Status | Scenario              |
|--------|-----------------------|
| 404    | Product not found     |

---

### 6. Update Product Rating

Updates `rating` and `reviewCount` for a product. Intended to be called by the Reviews service when a new review is submitted — not an admin or end-user operation.

```
POST /api/products/{productId}/rating?rating={rating}&reviewCount={reviewCount}
```

**Query Params**

| Param         | Required | Description                        |
|---------------|----------|------------------------------------|
| `rating`      | Yes      | New average rating (e.g. `4.6`)    |
| `reviewCount` | Yes      | Total number of reviews (e.g. `135`) |

**Success Response — 200 OK** — returns full updated `ProductVO`

**Error Responses**

| Status | Scenario                           |
|--------|------------------------------------|
| 404    | Product not found or is `INACTIVE` |

---

### 7. Get All Categories

Returns all categories ordered by `displayOrder`. Use `parentCategoryId` to reconstruct the hierarchy tree on the client side.

```
GET /api/products/categories
```

**Success Response — 200 OK**

```json
[
  {
    "categoryId": 1,
    "name": "Electronics",
    "description": "Electronic devices and accessories",
    "parentCategoryId": null,
    "imageUrl": "https://images.example.com/categories/electronics.jpg",
    "displayOrder": 1
  },
  {
    "categoryId": 3,
    "name": "Mobile Phones",
    "description": "Smartphones and accessories",
    "parentCategoryId": 1,
    "imageUrl": "https://images.example.com/categories/mobiles.jpg",
    "displayOrder": 3
  }
]
```

---

## Dynamic Discount — How It Works

Discounts are stored in the `DISCOUNTS` table with `startDate` and `endDate`. At query time, the service checks if `NOW()` falls within an active discount window for that product:

```
discountPrice = price - (price × discountPct / 100)
```

- No restart required to activate/deactivate a discount — just insert/update a row in `DISCOUNTS`
- `discountPrice` is `null` in the response if no active discount exists
- The maximum allowed discount percentage is controlled by `discount.max-discount-pct` in `application.yaml` (default `90%`), refreshable at runtime via Spring Cloud Config without restart using `POST /actuator/refresh`

---

## Error Response Shape

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Product not found with id: 99",
  "path": "/api/products/99",
  "timestamp": "2024-01-15T10:30:00"
}
```

| Status | Error                 | Trigger                              |
|--------|-----------------------|--------------------------------------|
| 404    | Not Found             | Product or category does not exist   |
| 409    | Conflict              | Duplicate SKU on create              |
| 500    | Internal Server Error | Unhandled exception                  |

---

## Key Design Decisions

- **SKU as natural unique key**: Every product variant has a unique SKU (e.g. `SAMSUNG-S24-BLK-128`). Used as the join key between `PRODUCTS` and `INVENTORY` tables.
- **Toggle status instead of delete**: `toggleStatus` flips `ACTIVE` ↔ `INACTIVE` in a single endpoint. The row is never removed — past orders referencing `productId` remain intact. Admin can relist a product without any extra endpoint.
- **Images in separate table**: `PRODUCT_IMAGES` joined by `productId`. Supports multiple images per product with `displayOrder` and `isPrimary` flags. Update replaces all images atomically.
- **Dynamic discount**: Calculated at query time from `DISCOUNTS` table — no stored `discountPrice` column. Activate/deactivate by inserting rows with date ranges.
- **Max discount cap**: Configurable via `discount.max-discount-pct` in Spring Cloud Config. Refreshable at runtime via `POST /actuator/refresh` — no restart needed.
- **Category hierarchy**: Flat list with `parentCategoryId` self-reference. Client reconstructs the tree. `displayOrder` controls UI sort order.

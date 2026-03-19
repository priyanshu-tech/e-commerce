# Payment API Documentation

## Overview

Handles payment processing via **mock Razorpay implementation**. Uses a 2-step flow — create a mock Razorpay order first, then verify with a hardcoded signature check. No real Razorpay SDK calls are made — designed for local testing without Razorpay credentials.

---

## Base URL

```
http://localhost:8097/api/payments
```

---

## Mock Mode — No Setup Required

No Razorpay credentials needed. The implementation generates a mock `razorpayOrderId` and uses a hardcoded signature check (`"valid"` = success).

---

## Payment Flow

```
1. POST /api/payments/create-order
        │
        ▼
   Mock razorpayOrderId generated (mock_order_xxxxx)
        │
        ▼
   PAYMENTS row saved (status = PENDING)
        │
        ▼
2. POST /api/payments/verify
        │
        ▼
   razorpaySignature == "valid" ?
        │
   ┌────┴────┐
  yes       no
   │           │
PAYMENTS    PAYMENTS
SUCCESS     FAILED
ORDER → PAID
```

---

## Data Models

### Payment

| Field                | Type          | Notes                                              |
|----------------------|---------------|----------------------------------------------------|
| `paymentId`          | Long          | Surrogate PK, auto-generated via `PAYMENTS_SEQ`   |
| `orderId`            | Long          | FK → ORDERS, unique — one payment per order       |
| `orderNumber`        | String        | e.g. `ORD-20250101-0001`                          |
| `razorpayOrderId`    | String        | Mock-generated on create-order (`mock_order_xxx`) |
| `razorpayPaymentId`  | String        | Provided by caller on verify, null until then     |
| `amount`             | Long          | In paise (INR × 100)                              |
| `currency`           | String        | Default `INR`                                     |
| `status`             | String        | `PENDING` → `SUCCESS` / `FAILED` / `REFUNDED`    |
| `paymentDate`        | LocalDateTime | Set on insert, never updated                      |

### PaymentVO (Response)

Same fields as above — entity directly mapped to VO.

---

## Endpoints

### 1. Create Razorpay Order

Creates a Razorpay order and saves a `PENDING` payment record. Returns `razorpayOrderId` which the frontend uses to open the Razorpay checkout popup.

```
POST /api/payments/create-order?orderNumber={orderNumber}&currency=INR
```

**Query Params**

| Param         | Required | Default | Description                        |
|---------------|----------|---------|------------------------------------|
| `orderNumber` | Yes      | —       | e.g. `ORD-20250101-0001`           |
| `currency`    | No       | `INR`   | Payment currency                   |

**Success Response — 201 Created**

```json
{
  "paymentId": 1,
  "orderId": 1,
  "orderNumber": "ORD-20250101-0001",
  "razorpayOrderId": "mock_order_SSd7jaq9IM2aCj",
  "razorpayPaymentId": null,
  "amount": 89997,
  "currency": "INR",
  "status": "PENDING",
  "paymentDate": "2025-01-01T10:30:00"
}
```

**Error Responses**

| Status | Scenario                                          |
|--------|---------------------------------------------------|
| 404    | Order not found for orderNumber                   |
| 500    | Payment already initiated for this order          |

---

### 2. Verify Payment

Mock signature check — pass `razorpaySignature=valid` for SUCCESS, anything else marks payment as `FAILED`. On success — payment status → `SUCCESS`, order status → `PAID`.

```
POST /api/payments/verify?razorpayOrderId={id}&razorpayPaymentId={id}&razorpaySignature={sig}
```

**Query Params**

| Param                | Required | Description                                      |
|----------------------|----------|--------------------------------------------------|
| `razorpayOrderId`    | Yes      | From `create-order` response                     |
| `razorpayPaymentId`  | Yes      | Any value e.g. `pay_test_123` (mock)             |
| `razorpaySignature`  | Yes      | Pass `valid` for SUCCESS, anything else = FAILED |

**Success Response — 200 OK (signature valid)**

```json
{
  "paymentId": 1,
  "orderId": 1,
  "orderNumber": "ORD-20250101-0001",
  "razorpayOrderId": "mock_order_SSd7jaq9IM2aCj",
  "razorpayPaymentId": "pay_test_123",
  "amount": 89997,
  "currency": "INR",
  "status": "SUCCESS",
  "paymentDate": "2025-01-01T10:30:00"
}
```

**Success Response — 200 OK (signature invalid)**

```json
{
  ...
  "status": "FAILED"
}
```

**Error Responses**

| Status | Scenario                                              |
|--------|-------------------------------------------------------|
| 404    | No payment found for razorpayOrderId                  |

---

### 3. Get Payment by Order Number

Fetches the payment record for a given order.

```
GET /api/payments/{orderNumber}
```

**Path Variable**

| Variable      | Description              |
|---------------|--------------------------|
| `orderNumber` | e.g. `ORD-20250101-0001` |

**Success Response — 200 OK** — returns PaymentVO (same shape as above)

**Error Responses**

| Status | Scenario                              |
|--------|---------------------------------------|
| 404    | Payment not found for orderNumber     |

---

### 4. Refund Payment

Initiates a full refund via Razorpay. Only `SUCCESS` payments can be refunded.

```
POST /api/payments/{orderNumber}/refund
```

**Path Variable**

| Variable      | Description              |
|---------------|--------------------------|
| `orderNumber` | e.g. `ORD-20250101-0001` |

**Success Response — 200 OK**

```json
{
  "status": "success",
  "message": "Refund initiated for order ORD-20250101-0001"
}
```

**Error Responses**

| Status | Scenario                                          |
|--------|---------------------------------------------------|
| 404    | Payment not found for orderNumber                 |
| 500    | Payment is not in SUCCESS status                  |


---

## Error Response Shape

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Payment not found for order: ORD-20250101-0099",
  "path": "/api/payments/ORD-20250101-0099",
  "timestamp": "2025-01-01T10:30:00"
}
```

---

## Key Design Decisions

- **Mock implementation**: No real Razorpay SDK calls. `razorpayOrderId` is generated as `mock_order_` + random 14-char UUID fragment.
- **Mock signature check**: `razorpaySignature == "valid"` → SUCCESS, anything else → FAILED. Replaces real HMAC-SHA256 verification.
- **Amount in paise**: `totalAmount` from `ORDERS` is multiplied by 100 — kept consistent with real Razorpay contract.
- **One payment per order**: `ORDER_ID` is `UNIQUE` in `PAYMENTS` — duplicate payment creation throws an error.
- **Order status → PAID**: On successful verification, `ORDERS.ORDER_STATUS` is updated to `PAID` in the same transaction.
- **Full refund only**: Only `SUCCESS` payments can be refunded. Status flipped to `REFUNDED`. Partial refunds out of scope.

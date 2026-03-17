# User API Documentation

## Overview

Manages user profiles and their saved addresses. The `USERS` table uses a surrogate numeric `userId` as PK, auto-generated via Oracle sequence. `username` and `email` are individually unique. User lookup uses `?username=&email=` query params. Address operations use `userId` as path variable.

---

## Base URL

```
http://localhost:8097/api/users
```

---

## Data Models

### User

| Field         | Type          | Notes                                          |
|---------------|---------------|------------------------------------------------|
| `userId`      | Long          | Surrogate PK, auto-generated via `USERS_SEQ`  |
| `username`    | String        | Unique, immutable                              |
| `email`       | String        | Unique, immutable                              |
| `firstName`   | String        | Required                                       |
| `lastName`    | String        | Required                                       |
| `phoneNumber` | String        | Optional                                       |
| `status`      | String        | Defaults to `ACTIVE` on create                 |
| `createdAt`   | LocalDateTime | Set on insert, never updated                   |

### Address

| Field          | Type    | Notes                                                                          |
|----------------|---------|--------------------------------------------------------------------------------|
| `addressId`    | Long    | Surrogate PK, auto-generated via `ADDRESSES_SEQ`. Used as join key on `ORDERS` |
| `userId`       | Long    | FK → USERS                                                                     |
| `addressLine1` | String  | Required                                                                       |
| `addressLine2` | String  | Optional                                                                       |
| `city`         | String  | Required                                                                       |
| `state`        | String  | Required                                                                       |
| `zipCode`      | String  | Required                                                                       |
| `country`      | String  | Required                                                                       |
| `addressType`  | String  | e.g. `SHIPPING`, `BILLING`                                                     |
| `isDefault`    | Boolean | Only one address per user can be default. Defaults to `false`                  |

---

## Endpoints

### 1. Get User

Fetches a user's profile by `username` and `email`.

```
GET /api/users?username={username}&email={email}
```

**Query Params**

| Param      | Required | Description     |
|------------|----------|-----------------|
| `username` | Yes      | User's username |
| `email`    | Yes      | User's email    |

**Success Response — 200 OK**

```json
{
  "userId": 1,
  "username": "raj_kumar",
  "email": "raj.kumar@example.com",
  "firstName": "Raj",
  "lastName": "Kumar",
  "phoneNumber": "+919876543210",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Error Responses**

| Status | Scenario                              |
|--------|---------------------------------------|
| 404    | User not found                        |
| 400    | Missing `username` or `email` param   |

---

### 2. Create User

Creates a new user. `username` and `email` must each be unique. `createdAt` and `updatedAt` are set automatically via `@PrePersist`.

```
POST /api/users
```

**Request Body**

```json
{
  "username": "raj_kumar",
  "email": "raj.kumar@example.com",
  "firstName": "Raj",
  "lastName": "Kumar",
  "phoneNumber": "+919876543210",
  "status": "ACTIVE"
}
```

**Success Response — 201 Created**

```json
{
  "userId": 1,
  "username": "raj_kumar",
  "email": "raj.kumar@example.com",
  "firstName": "Raj",
  "lastName": "Kumar",
  "phoneNumber": "+919876543210",
  "status": "ACTIVE",
  "createdAt": "2024-01-15T10:30:00"
}
```

**Error Responses**

| Status | Scenario                                    |
|--------|---------------------------------------------|
| 409    | `username` already exists                   |
| 409    | `email` already registered with another user |

---

### 3. Update User

Updates mutable fields of an existing user. `username`, `email`, and `createdAt` cannot be changed.

```
POST /api/users/update?username={username}&email={email}
```

**Query Params**

| Param      | Required | Description     |
|------------|----------|-----------------|
| `username` | Yes      | User's username |
| `email`    | Yes      | User's email    |

**Request Body** — only these fields are applied:

```json
{
  "firstName": "Rajesh",
  "lastName": "Kumar",
  "phoneNumber": "+919999999999",
  "status": "ACTIVE"
}
```

**Success Response — 200 OK** — returns updated user (same shape as Get User response)

**Error Responses**

| Status | Scenario       |
|--------|----------------|
| 404    | User not found |

---

### 4. Get User Addresses

Returns all saved addresses for a user.

```
GET /api/users/{userId}/addresses
```

**Success Response — 200 OK**

```json
[
  {
    "addressId": 1,
    "userId": 1,
    "addressLine1": "Flat 301, Sunrise Apartments",
    "addressLine2": "MG Road",
    "city": "Bangalore",
    "state": "Karnataka",
    "zipCode": "560001",
    "country": "India",
    "addressType": "SHIPPING",
    "isDefault": true
  }
]
```

**Error Responses**

| Status | Scenario       |
|--------|----------------|
| 404    | User not found |

---

### 5. Add Address

Adds a new address for a user. If `isDefault: true`, any existing default address is automatically unset first.

```
POST /api/users/{userId}/addresses
```

**Request Body**

```json
{
  "addressLine1": "Plot 45, Sector 18",
  "addressLine2": "Near Metro Station",
  "city": "Gurgaon",
  "state": "Haryana",
  "zipCode": "122001",
  "country": "India",
  "addressType": "BILLING",
  "isDefault": false
}
```

> `addressId` is auto-generated by `ADDRESSES_SEQ` — do not pass it in the request.

**Success Response — 201 Created** — returns saved address with generated `addressId`

**Error Responses**

| Status | Scenario       |
|--------|----------------|
| 404    | User not found |

---

## Error Response Shape

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "User not found with userId: 99",
  "path": "/api/users/99/addresses",
  "timestamp": "2024-01-15T10:30:00"
}
```

| Status | Error                 | Trigger                            |
|--------|-----------------------|------------------------------------|
| 400    | Missing Parameter     | Required query param absent        |
| 404    | Not Found             | User or resource does not exist    |
| 409    | Conflict              | Duplicate username or email        |
| 500    | Internal Server Error | Unhandled exception                |

---

## Key Design Decisions

- **Surrogate userId**: `USERS` table now has a numeric `userId` PK via `USERS_SEQ`. `username` and `email` remain individually unique — no data integrity loss, but FK references from `CARTS`, `ORDERS` etc. are now clean single-column joins.
- **User lookup still by username + email**: `GET /api/users?username=&email=` — keeps the natural identity check for login-style lookups.
- **Address endpoints use userId path variable**: `GET /api/users/{userId}/addresses` and `POST /api/users/{userId}/addresses` — cleaner than passing username + email as query params on every address call.
- **Single default address**: Adding an address with `isDefault: true` automatically flips the previous default to `false` — no manual cleanup needed by the caller.
- **Immutable fields**: `username`, `email`, `createdAt` are never updated. The update endpoint only applies `firstName`, `lastName`, `phoneNumber`, `status`.
- **Single error logging point**: All exceptions propagate to `GlobalExceptionHandler` — no duplicate logs across service/mapper layers.

# TableSync

TableSync is a table booking management REST API for restaurants, cafes, or any place that takes table reservations. It aims to simplify the booking process for business owners and their customers.

This is an API-only project (no frontend), built as a personal learning project.

## Tech Stack

- Java 21 (LTS)
- Spring Boot 4.1.x (Spring Web MVC, Spring Data JPA, Spring Security, Bean Validation)
- PostgreSQL 16
- Flyway (versioned database migrations)
- JWT authentication (jjwt)
- Docker / Docker Compose (database and application)
- Maven, Lombok

## Features

- Register and log in with JWT authentication
- Browse tables and view a table's public booking schedule (no login required)
- Book, view, and cancel reservations
- Reservation lifecycle management for staff (confirm, complete, reject)
- Table management (admin)
- User management (admin)
- Role-based access control: `CUSTOMER`, `HOST`, `ADMIN`
- Consistent JSON error responses
- Double-booking protection at both the application and database level

## Roles and Permissions

| Action | Customer | Host | Admin |
|---|---|---|---|
| Register / log in | Yes | Yes | Yes |
| Browse tables and schedules | Yes (public) | Yes | Yes |
| Create a reservation | Yes | No | No |
| View reservations | Own only | All | All |
| Cancel a reservation | Own only | Any | Any |
| Update reservation status | No | Yes | Yes |
| Table create / update / delete | No | No | Yes |
| User management | No | No | Yes |

`POST /api/v1/auth/register` always creates a `CUSTOMER`. Host and admin accounts are created by an admin through the users endpoints.

## Reservation Lifecycle

```
PENDING -> CONFIRMED -> COMPLETED
```

- `CANCELLED`: set when a reservation is cancelled (customer, host, or admin).
- `REJECTED`: set by staff when a pending request is declined.
- `COMPLETED`, `CANCELLED`, and `REJECTED` are terminal states and cannot be changed or cancelled again.
- Only `PENDING` and `CONFIRMED` reservations block a time slot.

## Getting Started

### Prerequisites

- JDK 21 (make sure `JAVA_HOME` is set)
- Docker and Docker Compose

### 1. Configure environment variables

Create a `.env` file in the project root (see `.env.example`):

```
APP_PORT=8080
POSTGRES_PORT=5432
POSTGRES_DB=tablesync
POSTGRES_USER=tablesync_user
POSTGRES_PASSWORD=change_me
JWT_SECRET=<base64 string of at least 32 random bytes>
JWT_EXPIRATION_MS=3600000
```

Generate a secret with:

```
openssl rand -base64 32
```

Do not commit `.env`. It is listed in `.gitignore`.

### 2. Run the application

**Option A: everything in Docker (recommended)**

```
docker compose up -d --build
```

This builds the application image (multi-stage `Dockerfile`), starts PostgreSQL, waits for its healthcheck to pass, then starts the app. Follow the logs with `docker compose logs -f app`.

**Option B: database in Docker, app from your IDE**

```
docker compose up -d postgres
./mvnw spring-boot:run
```

Stop the `app` container first if it is running, since both use the same port.

The `dev` profile is active by default. Flyway runs the migrations in `src/main/resources/db/migration` on startup, and Hibernate only validates the schema (`ddl-auto: validate`).

The API is available at `http://localhost:8080/api/v1` (or the `APP_PORT` you configured).

Inside Compose, the app connects to the database through `SPRING_DATASOURCE_URL` (host `postgres`, port `5432`), which overrides the `localhost` URL in `application.yml`.

### 3. Create the first admin

Registration only creates customers, so promote one directly in the database:

```
docker exec -it tablesync-database psql -U tablesync_user -d tablesync \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'you@example.com';"
```

Log in again afterwards. The role is embedded in the JWT, so an old token keeps the old role.

## API Overview

All routes are prefixed with `/api/v1`. Protected routes expect an `Authorization: Bearer <token>` header.

### Auth

| Method | Route | Description | Access |
|---|---|---|---|
| POST | `/auth/register` | Register a new customer | Public |
| POST | `/auth/login` | Log in and receive a JWT | Public |

### Tables

| Method | Route | Description | Access |
|---|---|---|---|
| GET | `/tables` | List tables (paginated) | Public |
| GET | `/tables/{id}` | View a table | Public |
| GET | `/tables/{id}/schedules` | Booked time slots for a table | Public |
| POST | `/tables` | Create a table | Admin |
| PUT | `/tables/{id}` | Replace a table's details | Admin |
| DELETE | `/tables/{id}` | Delete a table | Admin |

`/tables/{id}/schedules` accepts optional `start` and `end` query parameters (ISO date-time, e.g. `2026-10-10T00:00:00`). They default to the current day, and the range may not exceed one week. It returns only start and end times, never customer details.

### Reservations

| Method | Route | Description | Access |
|---|---|---|---|
| GET | `/reservations` | Own reservations (customer) or all (host/admin), paginated | Authenticated |
| POST | `/reservations` | Create a reservation (starts as `PENDING`) | Customer |
| DELETE | `/reservations/{id}` | Cancel a reservation | Customer (own), Host, Admin |
| PATCH | `/reservations/{id}` | Update reservation status | Host, Admin |

Example booking request:

```json
{
  "tableId": "<table uuid>",
  "reservationStart": "2026-10-10T19:00:00",
  "reservationEnd": "2026-10-10T21:00:00"
}
```

### Users

| Method | Route | Description | Access |
|---|---|---|---|
| GET | `/users` | List users (paginated) | Admin |
| POST | `/users` | Create a user with any role | Admin |
| GET | `/users/{id}` | View a user | Admin |
| PUT | `/users/{id}` | Update a user's email, phone, and role | Admin |
| DELETE | `/users/{id}` | Delete a user | Admin |

### Pagination

List endpoints accept `page` (default `0`), `size` (default `10`), `sortBy` (default `createdAt`), and `sortDirection` (default `DESC`).

## Error Format

Errors share one JSON shape:

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "One field or more is invalid.",
    "details": [
      { "field": "email", "issue": "must be a well-formed email address" }
    ]
  }
}
```

Status codes follow these rules: `400` for malformed or invalid requests, `401` for missing or invalid credentials, `403` for insufficient permissions, `404` for missing resources, and `409` for conflicts with the current state (duplicate email, table already reserved, invalid status transition).

## Double-Booking Protection

Overlapping bookings are blocked in two layers:

1. **Application check:** `ReservationValidator` queries for overlapping `PENDING` or `CONFIRMED` reservations on the same table and returns a clean `409`.
2. **Database constraint:** a PostgreSQL exclusion constraint (`no_overlapping_reservations`, added in migration `V2`) guarantees no overlap even when two requests race. A violation is mapped to the same `409` response.

## Project Structure

```
src/main/java/com/alvinskylers/tablesync
├── controller/   REST endpoints
├── service/      business logic
├── validator/    reservation and table rules
├── repository/   Spring Data JPA repositories
├── entity/       JPA entities and enums
├── dto/          request and response objects
├── mapper/       entity to DTO mapping
├── security/     JWT service, filter, security config
└── exception/    custom exceptions and global handler
```

## Roadmap

- Refresh tokens and a real logout flow
- First-admin bootstrap that does not require manual SQL
- Automated tests
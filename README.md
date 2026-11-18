# Green Credit Programme (GCP) — Backend Service

Enterprise REST API for the **Green Credit Programme (GCP)** National Portal, built with **Java 17**, **Spring Boot 3.3.5**, **Spring Security 6 (Stateless JWT + RBAC)**, and a **Hybrid PostgreSQL 16 + MongoDB 7.0** persistence architecture synchronized via the **Transactional Outbox Pattern**.

---

## 1. Architecture Overview

### Core Stack
- **Runtime & Framework**: Java 17, Spring Boot `3.3.5` (`spring-boot-starter-web`, `spring-boot-starter-validation`, `@EnableScheduling`)
- **Security & Multi-Tenancy**:
  - **Stateless JWT Authentication**: `JwtUtil` + `JwtAuthenticationFilter` issuing signed HMAC-SHA256 bearer tokens (`role`, `companyId`, `companyStatus` claims).
  - **Password Cryptography**: `BCryptPasswordEncoder` for all corporate and administrative credentials.
  - **Role-Based & Tenant-Scoped Authorization**: Method-level security (`@EnableMethodSecurity`) enforcing `ROLE_ADMIN` for nodal officer queues and `@tenantSecurity.canAccessCompany(authentication, #id)` (`TenantSecurityEvaluator`) to prevent horizontal privilege escalation (IDOR) across company accounts.
- **Hybrid Persistence Architecture**:
  - **PostgreSQL 16 (Spring Data JPA / Hibernate)**: Stores relational, transactional, and financial state requiring strict ACID guarantees and pessimistic row-level locking:
    - `users` & `companies` (Statutory CIN, GSTIN, PAN, and verification status lifecycle)
    - `land_applications` (Afforestation proposals, species plan, sapling counts, DPR references, and plantation execution status)
    - `parcel_allocations` (Authoritative land reservation lock table with unique `parcel_id` constraint and `LockModeType.PESSIMISTIC_WRITE`)
    - `monitoring_records` (Monthly satellite/AI passes and field inspection state machine)
    - `credit_entries` & `credit_ledger_movements` (Immutable append-only ledger tracking credit issuance, holds, settlements, and releases)
    - `redemptions` (Corporate offset/compliance redemption requests and settlement records)
    - `audit_logs` (Tamper-evident regulatory audit trail)
    - `outbox_events` (Durable PostgreSQL transactional outbox queue)
  - **MongoDB 7.0 (Spring Data MongoDB)**: Stores geospatial and physical land parcel documents (`land_parcels` collection: parcel IDs like `GCP-MH-2026-001`, coordinates `lat`/`lng`, soil type, water availability, degradation level, and allocation metadata).
- **Transactional Outbox & Cross-Store Reconciliation**:
  - `OutboxService` writes `PARCEL_ALLOCATED` and `PARCEL_RELEASED` events into PostgreSQL within the same ACID transaction (`Propagation.MANDATORY`) as the application decision, and a background `@Scheduled(fixedDelay = 5000)` dispatcher synchronizes the status into MongoDB with exponential backoff.
  - `ReconciliationService` (`POST /api/admin/reconcile`) audits PostgreSQL `ParcelAllocation` rows against MongoDB `LandParcel` documents and repairs any cross-store drift.

---

## 2. 12-Step Domain Workflow

| Step | Domain Stage | Implementation & Persistence |
| :--- | :--- | :--- |
| **1** | **Company Onboarding & Statutory Submission** | `POST /api/companies/register` validates regex rules for **CIN** (`21 chars`), **GSTIN** (`15 chars`), and **PAN** (`10 chars`), creating a `Company` (`UNDER_VERIFICATION`) and linked `User` (`ROLE_COMPANY`). |
| **2** | **Admin Statutory Verification** | `PUT /api/admin/companies/{id}/verification` allows MoEFCC nodal officers (`ROLE_ADMIN`) to transition companies to `VERIFIED` or `REJECTED` with audited remarks. |
| **3** | **Eligible Land Parcel Registry & Browsing** | `GET /api/parcels` queries MongoDB for eligible degraded forest blocks by state, area, degradation level, and `AVAILABLE` status; admins register parcels via `POST /api/admin/parcels`. |
| **4** | **Rich Afforestation Application Submission** | `POST /api/applications` enforces `CompanyStatus.VERIFIED`, supports idempotency keys (`idempotencyKey`), and persists `proposedSpecies`, `saplingCount`, `plantingPlanSummary`, and `supportingDocRef`. |
| **5** | **Administrative Application Review** | `PUT /api/admin/applications/{id}/decision` supports `APPROVED`, `CHANGES_REQUESTED`, and `REJECTED` decisions with officer remarks. |
| **6** | **Atomic Land Reservation & Conflict Rejection** | On approval, `ApplicationService.decide()` acquires a pessimistic lock on `ParcelAllocation`, inserts an `ACTIVE` lock for the parcel, automatically rejects competing `PENDING_REVIEW` applications for that parcel, and enqueues a `PARCEL_ALLOCATED` outbox event. |
| **7** | **Plantation Execution & Activity Logging** | `PUT /api/applications/{id}/activity` allows the owning company (or admin) to update `plantationStatus` (`NOT_STARTED` &rarr; `PLANTING_IN_PROGRESS` &rarr; `PLANTED` &rarr; `UNDER_MONITORING`), `saplingCount`, and `lastActivityNote`. Also transitions `CHANGES_REQUESTED` applications back to `PENDING_REVIEW` on resubmission. |
| **8** | **Pluggable Satellite & AI Monitoring** | `SatelliteMonitoringProvider` interface with `MockSatelliteMonitoringProvider` (`@Primary`, `providerName = "MOCK_SENTINEL2_AI"`, `isSimulated = true`). Triggered via `POST /api/parcels/{id}/monitoring/trigger-mock`, `POST /api/admin/monitoring/trigger/{parcelId}`, or the monthly `@Scheduled` job. |
| **9** | **Field Inspection & Verification Gate** | `PUT /api/admin/monitoring/{id}/inspection` allows the nodal verifier to review satellite findings and set `inspectionStatus` to `INSPECTION_APPROVED` or `INSPECTION_REJECTED` with field remarks. |
| **10** | **Versioned Green Credit Policy Assessment** | `POST /api/admin/credits/calculate` computes draft credits using versioned policy `GCP-PILOT-2026-V1: Area(ha) x CanopyGrowth(%) [Simulated]`, enforcing a Field Inspection Gate (requiring an `INSPECTION_APPROVED` record if monitoring data exists) and preventing duplicate open drafts. |
| **11** | **Immutable Credit Issuance Ledger** | `PUT /api/admin/credits/{id}/decision` uses `findByIdForUpdate` (`PESSIMISTIC_WRITE`), prevents double-issuance per parcel, transitions the entry to `APPROVED`, and appends an immutable `CreditLedgerMovement` (`ISSUED`). |
| **12** | **Credit Redemption Hold & Settlement** | `POST /api/redemptions` places an immediate hold (`REDEMPTION_HOLD`) on `account.available` balance while in `REQUESTED` status. `PUT /api/admin/redemptions/{id}/decision` settles (`REDEMPTION_SETTLED`) or releases (`REDEMPTION_RELEASED`) the held credits. |

---

## 3. REST API Endpoint Summary

| Method | Endpoint | Auth / Role | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/health` | Public | Container liveness & health probe |
| `POST` | `/api/auth/login` | Public | Authenticate user & issue stateless JWT |
| `POST` | `/api/companies/register` | Public | Onboard company + statutory details + user account |
| `GET` | `/api/parcels` | Public / Auth | Filter land parcels (`state`, `degradation`, `status`) |
| `GET` | `/api/parcels/{id}/monitoring` | Auth | List satellite & field monitoring records for a parcel |
| `POST` | `/api/parcels/{id}/monitoring/trigger-mock` | Auth (`COMPANY`/`ADMIN`) | Run simulated `MOCK_SENTINEL2_AI` monthly pass |
| `POST` | `/api/applications` | Auth (`COMPANY`/`ADMIN`) | Submit afforestation application (`VERIFIED` company) |
| `PUT` | `/api/applications/{id}/activity` | Auth (Owner/`ADMIN`) | Update plantation progress or resubmit after changes requested |
| `GET` | `/api/companies/{id}/applications` | Auth (`@tenantSecurity`) | List enriched applications for company |
| `GET` | `/api/companies/{id}/parcels` | Auth (`@tenantSecurity`) | List allotted land parcels for company |
| `GET` | `/api/companies/{id}/credits` | Auth (`@tenantSecurity`) | Get credit balances (`totalEarned`, `redeemed`, `held`, `available`, `inPipeline`) |
| `GET` | `/api/companies/{id}/credits/ledger` | Auth (`@tenantSecurity`) | List company credit issuance entries |
| `GET` | `/api/companies/{id}/redemptions` | Auth (`@tenantSecurity`) | List company redemption requests |
| `POST` | `/api/redemptions` | Auth (`COMPANY`/`ADMIN`) | Submit credit redemption request |
| `GET` | `/api/admin/companies` | `ROLE_ADMIN` | List all registered companies |
| `PUT` | `/api/admin/companies/{id}/verification` | `ROLE_ADMIN` | Verify or reject company statutory credentials |
| `GET` | `/api/admin/parcels` | `ROLE_ADMIN` | List all land parcels across states |
| `POST` | `/api/admin/parcels` | `ROLE_ADMIN` | Register new land parcel in MongoDB |
| `GET` | `/api/admin/applications` | `ROLE_ADMIN` | List enriched applications (`companyName`, `parcelLocation`, etc.) |
| `PUT` | `/api/admin/applications/{id}/decision` | `ROLE_ADMIN` | Approve, Request Changes, or Reject application |
| `GET` | `/api/admin/monitoring` | `ROLE_ADMIN` | List all satellite/AI passes & inspection statuses |
| `POST` | `/api/admin/monitoring/trigger/{parcelId}` | `ROLE_ADMIN` | Trigger simulated satellite scan for a parcel |
| `PUT` | `/api/admin/monitoring/{id}/inspection` | `ROLE_ADMIN` | Approve or reject field inspection |
| `GET` | `/api/admin/credits` | `ROLE_ADMIN` | List all credit assessment entries |
| `POST` | `/api/admin/credits/calculate` | `ROLE_ADMIN` | Calculate `DRAFT` credit assessment under `GCP-PILOT-2026-V1` |
| `PUT` | `/api/admin/credits/{id}/decision` | `ROLE_ADMIN` | Approve (issue to ledger) or Reject credit draft |
| `GET` | `/api/admin/redemptions` | `ROLE_ADMIN` | List corporate credit redemption requests |
| `PUT` | `/api/admin/redemptions/{id}/decision` | `ROLE_ADMIN` | Settle or reject credit redemption hold |
| `GET` | `/api/admin/audit` | `ROLE_ADMIN` | View regulatory audit trail |
| `POST` | `/api/admin/reconcile` | `ROLE_ADMIN` | Run PostgreSQL &harr; MongoDB consistency reconciliation |

---

## 4. Environment Variables Reference

| Variable | Default (Dev Profile) | Description |
| :--- | :--- | :--- |
| `SPRING_PROFILES_ACTIVE` | `dev` | Active Spring Boot profile (`dev`, `prod`, `test`) |
| `SERVER_PORT` | `8081` (`8080` in OCI container) | HTTP listening port for embedded Tomcat |
| `POSTGRES_URL` | `jdbc:postgresql://localhost:5432/gcp_db` | PostgreSQL JDBC connection URL |
| `POSTGRES_USER` | `gcp_user` | PostgreSQL database username |
| `POSTGRES_PASSWORD` | *(Set via `.env` or secret store)* | PostgreSQL database password |
| `MONGO_URI` | `mongodb://localhost:27017/gcp_mongo_db` | MongoDB connection URI |
| `JWT_SECRET` | *(Set via `.env` or secret store)* | Base64/32+ byte secret key for HMAC-SHA256 JWT signing |
| `JWT_EXPIRATION_MS` | `86400000` (24 hours) | JWT token validity duration in milliseconds |
| `GCP_CORS_ORIGINS` | `http://localhost:3000,http://localhost:5173` | Comma-separated allowed CORS origins |

---

## 5. Local Development & Test Suite

### Prerequisites
- **JDK 17+** (Maven Wrapper `./mvnw` / `mvnw.cmd` is bundled)
- **Docker Compose** (or local PostgreSQL 16 on `:5432` and MongoDB 7.0 on `:27017`)

### Step 1: Start PostgreSQL & MongoDB Containers
```bash
docker compose -f docker-compose.yaml up -d
```

### Step 2: Run the Backend Server
**Linux / macOS:**
```bash
./mvnw spring-boot:run
```
**Windows (PowerShell / CMD):**
```powershell
.\mvnw.cmd spring-boot:run
```
On startup in the `dev` profile (`gcp.seed-demo-data: true`), `DemoDataSeeder` automatically populates demo users, companies, 8 MongoDB land parcels, applications, 5 simulated `MOCK_SENTINEL2_AI` monitoring passes, credit ledger entries, and audit logs.

### Step 3: Run the Automated Test Suite (53 Tests)
The test suite uses `@ActiveProfiles("test")` (H2 in PostgreSQL compatibility mode + Mocked `MongoTemplate`) so it runs deterministically without external database dependencies:
```bash
./mvnw test
```
All **53 tests** across `AuthIntegrationTest`, `CompanyRegistrationValidationTest`, `SecurityAndTenantIsolationTest`, `FinancialGradeWorkflowTest`, `CrossStoreOutboxIntegrationTest`, and `GcpApplicationTests` pass.

---

## 6. OCI & Rootless Podman Deployment Guide

The `backend/Dockerfile` is engineered for **strict OCI compliance** and **rootless Podman** execution:
- Uses explicit fully-qualified registry names (`docker.io/library/eclipse-temurin:17-jdk-jammy` and `docker.io/library/eclipse-temurin:17-jre-jammy`).
- Runs as an unprivileged user and group (`UID 10001` / `GID 10001`, `USER 10001:10001`).
- Binds to unprivileged port `8080` (`>1024`).

### Build OCI Image with Rootless Podman
```bash
cd backend
podman build --format oci -t gcp-backend:1.0.0 .
```

### Verify Non-Root UID/GID Inside Container
```bash
podman run --rm --entrypoint id gcp-backend:1.0.0
# Expected output: uid=10001(gcp) gid=10001(gcp) groups=10001(gcp)
```

### Run Container in Rootless Podman
```bash
podman run -d \
  --name gcp-backend \
  --userns=keep-id:uid=10001,gid=10001 \
  -p 8081:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  -e SERVER_PORT=8080 \
  -e POSTGRES_URL="jdbc:postgresql://host.containers.internal:5432/gcp_db" \
  -e POSTGRES_USER="gcp_user" \
  -e POSTGRES_PASSWORD="<YOUR_DB_PASSWORD>" \
  -e MONGO_URI="mongodb://host.containers.internal:27017/gcp_mongo_db" \
  -e JWT_SECRET="<YOUR_256_BIT_JWT_SECRET>" \
  -e GCP_CORS_ORIGINS="http://localhost:3000" \
  gcp-backend:1.0.0
```

### Check Container Health
```bash
curl -fsS http://localhost:8081/api/health
```

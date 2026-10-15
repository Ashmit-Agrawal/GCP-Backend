# GCP Backend (initial version)

Java 17 + Spring Boot 3 REST API for the Green Credit Programme prototype.
Deliberately simple for now: **no security/login**, an in-memory H2 database, and just the core workflow.

## Run

```bash
mvn spring-boot:run
```

API: `http://localhost:8080/api` · H2 console: `http://localhost:8080/h2-console`
(JDBC URL `jdbc:h2:mem:gcpdb`, user `sa`, empty password).
Data resets on restart; 4 demo parcels and 1 demo company are loaded (`gcp.seed-demo-data` in `application.properties`).

CORS allows the Vite dev server at `http://localhost:5173`. Change `gcp.cors.allowed-origins` if yours differs.

## Layout

```
controller/   REST endpoints (thin)
service/      business rules (status changes, credit formula, balance checks)
repository/   Spring Data JPA
model/        entities + enums
dto/          request/response records
exception/    404 / 400 handling -> {"error": "...", "message": "..."}
config/       CORS, demo data
```

## Workflow in API calls

1. Government uploads parcel &rarr; `POST /api/parcels` (ID like `GCP-MH-2026-001` is generated)
2. Company registers &rarr; `POST /api/companies`
3. Company browses &rarr; `GET /api/parcels?status=AVAILABLE`
4. Company applies &rarr; `POST /api/applications` (status `PENDING_REVIEW`)
5. Officer decides &rarr; `PUT /api/applications/{id}/decision` (approve: parcel `ALLOTTED`, allotment date set)
6. Monitoring readings &rarr; `POST /api/parcels/{id}/monitoring`
7. Draft credits &rarr; `POST /api/credits/calculate` (Area x Canopy Growth %)
8. Officer approves credits &rarr; `PUT /api/credits/{id}/decision`
9. Company redeems &rarr; `POST /api/redemptions`, officer `PUT /api/redemptions/{id}/decision`
10. Every step is written to the audit log &rarr; `GET /api/audit`

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/health` | Is the server up |
| POST | `/api/companies` | Register company |
| GET | `/api/companies`, `/api/companies/{id}` | List / get |
| GET | `/api/companies/{id}/applications` | Company's applications |
| GET | `/api/companies/{id}/parcels` | Allocated land parcels |
| GET | `/api/companies/{id}/credits` | Account: totalEarned, redeemed, available, inPipeline |
| GET | `/api/companies/{id}/credits/ledger` | Credit ledger |
| GET | `/api/companies/{id}/redemptions` | Redemption history |
| GET | `/api/parcels` | Filters: `state, minArea, maxArea, degradation, status` |
| GET | `/api/parcels/{id}` | One parcel |
| POST | `/api/parcels` | Upload parcel |
| GET/POST | `/api/parcels/{id}/monitoring` | Canopy history / add reading |
| POST | `/api/applications` | Apply for a parcel |
| GET | `/api/applications?status=` | Officer queue |
| PUT | `/api/applications/{id}/decision` | Approve / reject |
| POST | `/api/credits/calculate` | Draft credit entry |
| GET | `/api/credits?status=` | Credit entries |
| PUT | `/api/credits/{id}/decision` | Approve (optionally revise) / reject |
| GET | `/api/redemptions/options` | Options and costs |
| POST | `/api/redemptions` | Request redemption |
| GET | `/api/redemptions?status=` | Redemption queue |
| PUT | `/api/redemptions/{id}/decision` | Approve / reject |
| GET | `/api/audit` | Audit trail (`entityType` + `entityId` to filter) |

Enum values: degradation `LOW|MEDIUM|HIGH` · parcel `AVAILABLE|ALLOTTED` · application `PENDING_REVIEW|APPROVED|REJECTED` ·
verification `AI_VERIFIED|AI_OFFICER_VERIFIED|MANUAL_INSPECTION` · credit `DRAFT|APPROVED|REJECTED` ·
redemption `REQUESTED|APPROVED|REJECTED` · options `CARBON_OFFSET_CERTIFICATE(100) | ESG_REPORTING_CLAIM(150) | COMPLIANCE_ADJUSTMENT(200) | NET_ZERO_NEUTRALIZATION(250)`.

## Try it (curl)

```bash
# 1. apply (demo company is id 1, first demo parcel is GCP-MH-<year>-001)
curl -X POST localhost:8080/api/applications -H 'Content-Type: application/json' \
  -d '{"companyId":1,"parcelId":"GCP-MH-2026-001"}'

# 2. officer approves
curl -X PUT localhost:8080/api/applications/1/decision -H 'Content-Type: application/json' \
  -d '{"approve":true,"officerName":"Officer Sharma"}'

# 3. draft credits: 18.5 ha x 20.1 = 371.85
curl -X POST localhost:8080/api/credits/calculate -H 'Content-Type: application/json' \
  -d '{"parcelId":"GCP-MH-2026-001","canopyGrowthPercent":20.1,"verificationMethod":"AI_OFFICER_VERIFIED"}'

# 4. officer approves credits, then check the account
curl -X PUT localhost:8080/api/credits/1/decision -H 'Content-Type: application/json' \
  -d '{"approve":true,"officerName":"Officer Sharma"}'
curl localhost:8080/api/companies/1/credits

# 5. redeem 250 GC
curl -X POST localhost:8080/api/redemptions -H 'Content-Type: application/json' \
  -d '{"companyId":1,"option":"NET_ZERO_NEUTRALIZATION"}'
```
(Parcel IDs contain the current year; use `GET /api/parcels` to see the real ones.)

## Not included yet (on purpose)

Authentication/roles, real satellite/AI integration, email notifications, pagination, and a PostgreSQL/MySQL setup.
To move off H2: replace the `h2` dependency in `pom.xml` with the PostgreSQL (or MySQL) driver and update the three
`spring.datasource.*` lines in `application.properties`.

## Credit formula

`CreditService.calculate(...)` uses the demo rule `area (ha) x canopy growth (%)`. The notified methodology awards credits
per surviving tree after five years, so that method is the single place to change later.

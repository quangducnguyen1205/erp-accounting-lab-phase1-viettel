# Final Web UI Design Plan

## Goal

Design a polished, product-like web console for the final Phase 1.5 demo before changing React code.

Working title:

```text
AI Knowledge Workspace — Phase 1.5 Ops Console
```

The UI should help a mentor see the full backend architecture flow without turning this repo into a frontend product:

```text
React Web UI
-> Keycloak login
-> Kong Gateway
-> tenant-demo master_data API
-> Kafka MasterDataChangedEvent
-> audit-log-service
-> audit API through Kong
-> Loki/Grafana and Kafka UI for inspection
```

## Design Direction

- Modern SaaS admin console, not a toy lab page.
- Desktop-first for laptop demo screens.
- Light theme by default.
- Subtle blue/indigo primary color with neutral gray surfaces.
- Sidebar navigation, top bar, status badges, readable tables and clear empty/error states.
- Implementation-friendly with the current Vite/React/plain CSS setup.
- No heavy component library, Redux/Zustand, drag/drop, realtime charts or websockets required.

The design should feel like an operations workspace for backend architecture learning: quiet, technical and easy to scan.

## Figma

Figma file:

[AI Knowledge Workspace - Phase 1.5 Ops Console](https://www.figma.com/design/kyPvlljW1KhQhoUvqLcjco)

Because the current Figma account is on a Starter plan, the file is organized into three pages instead of the originally requested five:

| Page | Purpose | Status |
|---|---|---|
| `00 Design System` | Color, typography, spacing and component patterns | Created in Figma |
| `01 Web App Screens + Demo Flow` | App screens and end-to-end demo flow map | Planned in this doc for next Figma pass |
| `02 Keycloak Theme + Handoff Notes` | Login theme concept and implementation notes | Planned in this doc for next Figma pass |

The Figma MCP call limit stopped the automated screen generation after the design-system page. This markdown spec is therefore the source of truth for the remaining screen design until the next manual or automated Figma pass.

Screen handoff and expected screenshot filenames are tracked in [final-web-ui-figma-screen-handoff.md](final-web-ui-figma-screen-handoff.md).

## Screen List

### 1. Welcome / Logged Out

Purpose: give context before Keycloak login.

Content:

- Product title: `AI Knowledge Workspace`.
- Subtitle: `Phase 1.5 Backend Architecture Demo`.
- Main action: `Sign in with Keycloak`.
- Local demo account hints:
  - `tenant1-user / password` — `ACCOUNTANT`.
  - `tenant2-user / password` — `VIEWER`.
- Short explanation that this is a local learning console and tokens are never displayed.

States:

- Guest.
- Auth initializing.
- Login error.
- Token refresh warning.

### 2. Dashboard / Architecture Overview

Purpose: orient the mentor before running API actions.

Sections:

- Stack cards:
  - Web UI.
  - Keycloak.
  - Kong Gateway.
  - `tenant-demo` / master-data service.
  - Kafka.
  - `audit-log-service`.
  - PostgreSQL.
  - Loki/Grafana.
  - Kafka UI.
- Current status:
  - API base URL.
  - selected gateway preset.
  - authenticated user.
  - `tenant_id`.
  - roles.
  - last requestId.
- Demo checklist:
  - Infrastructure running.
  - Logged in.
  - API base URL is Kong.
  - Create master data.
  - Audit event appears.
  - Kafka UI shows message.
  - Loki shows logs.
  - Tenant2 isolation verified.
  - Viewer create returns `403`.

### 3. Master Data

Purpose: primary business API demo.

Features:

- Load master data.
- Load by code for Redis cache-aside observation.
- Create master data.
- Unique code helper, for example `UI-DEMO-${timestamp}`.
- Status and requestId for each API call.
- Duplicate code state: `409 Conflict`.
- Viewer state: `403 Forbidden`.

Table fields:

- Code.
- Name.
- Type/category.
- Active/status.
- Updated/created timestamp if available.

Do not fake Redis hit/miss in the UI. The UI calls the endpoint; logs/metrics prove cache behavior.

### 4. Audit Events

Purpose: demonstrate cross-service Kafka flow.

Flow panel:

```text
Create master data
-> tenant-demo publishes Kafka event
-> audit-log-service consumes event
-> audit table
-> read audit API through Kong
```

Table fields should adapt to the real API response:

- eventId.
- tenantId.
- eventType.
- aggregateType.
- aggregateId.
- aggregateCode or code.
- changeType.
- occurredAt.
- consumedAt.

Important empty state:

- `tenant2-user` may get `HTTP 200` with an empty list.
- That is a successful tenant isolation proof, not a failure.

### 5. Observability

Purpose: show how to inspect the demo without making the UI depend on observability tools.

Cards/links:

- Grafana Loki: `http://localhost:13001`.
- Kafka UI: `http://localhost:18082`.
- Kong proxy: `http://localhost:18000`.
- Keycloak: `http://localhost:18080`.
- Optional metrics Grafana: `http://localhost:13000`.

`How to read logs` panel:

```logql
{service="tenant-demo"}
{service="audit-log-service"}
{service="kong-gateway"}
{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "requestId="
{service=~"tenant-demo|audit-log-service|kong-gateway"} |= "UI-DEMO"
{service=~"tenant-demo|kong-gateway"} |= "409"
{service=~"tenant-demo|kong-gateway"} |= "403"
```

Explain in UI copy:

- `service` and `source` are Loki labels.
- requestId, code and status are searched as text.
- Browser console errors are not Loki logs.

### 6. Error And Empty States

The implementation should have consistent, visible states for:

- `401 Unauthorized`: missing/expired token.
- `403 Forbidden`: authenticated but role does not allow action.
- `409 Conflict`: duplicate code in same tenant.
- `500 Unexpected`: backend issue, check requestId in logs.
- Service unavailable: gateway/upstream not reachable.
- Empty audit list: valid result for tenant isolation.

## User Flows

### ACCOUNTANT Flow

1. Open the app.
2. Sign in through Keycloak as `tenant1-user`.
3. Confirm `tenant_id=1` and role `ACCOUNTANT`.
4. Load master data.
5. Create unique master data.
6. Load audit events.
7. Open Kafka UI and inspect `master-data-events`.
8. Open Grafana Loki and query by code/requestId.
9. Try duplicate create and see `409 Conflict`.

### VIEWER Flow

1. Logout.
2. Sign in as `tenant2-user`.
3. Confirm `tenant_id=2` and role `VIEWER`.
4. Load master data.
5. Load audit events and confirm tenant1 events do not appear.
6. Try create master data and see `403 Forbidden`.

## Design System Tokens

### Colors

| Token | Value | Usage |
|---|---|---|
| Ink | `#0F172A` | Primary text/sidebar |
| Muted | `#64748B` | Secondary text |
| Canvas | `#F5F7FB` | App background |
| Surface | `#FFFFFF` | Panels/cards |
| Border | `#D8E0E8` | Dividers/inputs |
| Primary | `#2563EB` | Primary buttons/active nav |
| Indigo | `#4F46E5` | Secondary accent |
| Teal | `#0891B2` | Observability/event accent |
| Success | `#16A34A` | Success state |
| Warning | `#D97706` | Warning state |
| Danger | `#DC2626` | Error/destructive state |

### Typography

Use Inter.

| Role | Size | Weight | Usage |
|---|---:|---|---|
| Display | 32 | Bold | Page/product headers |
| Heading | 22 | Semi Bold | Section titles |
| Subheading | 18 | Semi Bold | Card titles |
| Body | 15 | Regular | Main UI text |
| Table/body small | 13 | Regular | Tables and dense panels |
| Meta | 12 | Medium | Badges, labels, timestamps |

### Layout

- Desktop frame: 1440px wide.
- Sidebar: 248px.
- Top bar: 76px.
- Content gutter: 44px.
- Card radius: 8-12px.
- Compact cards and tables; avoid marketing hero sections after login.

## Keycloak Login Theme Design Notes

The Keycloak login concept should match the web console but remain recognizable as a standard login form.

Layout:

- Left side:
  - `AI Knowledge Workspace`.
  - `Phase 1.5 Backend Architecture Demo`.
  - Short architecture explanation.
  - Simple stack map: Web UI, Keycloak, Kong, services, Kafka, Loki.
- Right side:
  - username field.
  - password field.
  - sign in button.
  - error message area.
  - local demo account hints.

Local account hint copy:

```text
Local demo only
tenant1-user / password - ACCOUNTANT
tenant2-user / password - VIEWER
```

Future implementation path:

```text
lab-code/keycloak-lab/themes/<theme-name>/login/theme.properties
lab-code/keycloak-lab/themes/<theme-name>/login/login.ftl
lab-code/keycloak-lab/themes/<theme-name>/login/resources/css/styles.css
```

Keycloak would mount the theme directory and the realm would set the custom login theme. Do not change auth logic for the theme.

## Implementation Phases

### Phase A: Layout Polish

- Replace single-page technical panels with sidebar + topbar shell.
- Keep current Keycloak auth helper and API functions.
- Add responsive constraints for laptop widths.
- Keep Docker-first web UI workflow.

### Phase B: Master Data Screen

- Move load/list/create/load-by-code into a dedicated screen.
- Add clearer status cards and 409/403/401 states.
- Keep the current API contracts.

### Phase C: Audit Events Screen

- Add audit table/timeline polish.
- Make empty `HTTP 200` visually successful for tenant2.
- Keep tenant isolation explanation close to the table.

### Phase D: Observability Screen

- Add local tool cards and LogQL recipes.
- Link to Grafana Loki and Kafka UI.
- Do not call Loki/Kafka UI APIs directly from the business UI.

### Phase E: Keycloak Login Theme

- Implement only after the React UI shell is approved.
- Keep public client + PKCE/local redirect settings.
- Do not put secrets in theme files.

## What Is Intentionally Not Included

- React Native or Expo.
- Heavy UI framework.
- Redux/Zustand.
- Real-time dashboards.
- Drag/drop or advanced animations.
- Direct UI calls to PostgreSQL, Redis, Kafka, MinIO, Prometheus or Grafana for business flow.
- Backend API contract changes.
- Production-grade frontend security hardening.

## Open Questions And Risks

- Figma Starter plan currently limits page count and MCP operations; the design system exists in Figma, while the full screen set is specified here for the next pass.
- Keycloak custom theme implementation may need careful testing after container theme mounting.
- If the UI later adds status probes for tools, keep them optional and local-only.
- Keep the app a teaching console, not a second product scope.

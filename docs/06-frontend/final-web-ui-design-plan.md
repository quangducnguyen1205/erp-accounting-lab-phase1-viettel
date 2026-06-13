# Final Web UI Design Plan

## Goal

Design the final Phase 1.5 web UI as a small business product, not as an architecture dashboard.

Product name:

```text
Master Data Portal
```

Positioning:

- A lightweight SaaS-style business app for tenant-scoped master data.
- Normal users manage shared business reference data and review activity history.
- Backend/infra technology remains real and demoable, but it is not the primary UI concept.
- The mentor can still hear the architecture explanation verbally and through the demo script.

The UI should keep the existing backend flow:

```text
React Web UI
-> Keycloak login
-> Kong Gateway
-> tenant-demo master_data API
-> Kafka MasterDataChangedEvent
-> audit-log-service
-> activity/audit API through Kong
```

## Design Guardrails

This redesign uses standard usability/accessibility guardrails rather than a heavy design system:

- Visibility of system status: every API action shows loading, success, requestId and error state.
- Match the real task: pages are named Dashboard, Master Data, Activity Log and Account, not by technology stack.
- Consistency: one sidebar, one topbar, consistent cards, badges, tables and alerts.
- Error prevention and recovery: duplicate code, forbidden create and expired/missing auth states are clear near the action.
- Minimalist design: infrastructure tools are available only as secondary demo notes, not primary navigation.
- Accessibility basics: readable contrast, clear labels, visible focus states and useful error copy.

Reference principles:

- Nielsen Norman Group: [10 Usability Heuristics for User Interface Design](https://www.nngroup.com/articles/ten-usability-heuristics/)
- W3C WAI: [Tips for Designing](https://www.w3.org/WAI/tips/designing/)

## Design Direction

- Modern SaaS admin interface.
- Light theme by default.
- Neutral gray surfaces with restrained blue/indigo accents.
- Sidebar navigation and compact topbar.
- Dense, readable business tables.
- Clear empty, loading, success and error states.
- Desktop-first for laptop demo, responsive enough for smaller screens.
- Implementable with Vite/React/plain CSS. No heavy UI framework, Redux/Zustand, drag/drop, realtime charts or WebSockets.

The UI should feel like a real internal portal that happens to be backed by the Phase 1.5 architecture.

## Figma

Existing Figma file:

[AI Knowledge Workspace - Phase 1.5 Ops Console](https://www.figma.com/design/kyPvlljW1KhQhoUvqLcjco)

Current limitation:

- The file name and early design-system page still reflect the old architecture-console direction.
- Figma Starter MCP tool-call limits currently block automated rename/screen generation.
- Until the next Figma pass, this markdown spec is the source of truth.

Next Figma pass should rename/reframe the design around:

```text
Master Data Portal
```

## Product Navigation

Primary navigation:

1. Dashboard
2. Master Data
3. Activity Log
4. Account

Optional secondary area:

- Developer Tools / Demo Notes may exist as a small footer link or account-side note.
- It must not be the main experience.

Remove or de-emphasize:

- Architecture Overview as the main page.
- Stack component cards as primary content.
- Web UI / Keycloak / Kong / Kafka / PostgreSQL cards as the product dashboard.
- `AI Knowledge Workspace`.
- `Phase 1.5 Demo Console` as product title.
- Observability as a primary user task.

## Screen Specification

### 1. Login / Welcome

Purpose: greet the user before Keycloak login.

Content:

- Product title: `Master Data Portal`.
- Subtitle: `Manage shared business data across tenants`.
- Primary button: `Sign in`.
- Short product copy: manage tenant-scoped reference records and review activity history.
- Local demo account hints:
  - `tenant1-user / password` - Accountant.
  - `tenant2-user / password` - Viewer.
- Note: tokens are never displayed.

States:

- Guest.
- Auth initializing.
- Login error.
- Token refresh warning.

### 2. Dashboard

Purpose: business overview, not architecture overview.

Cards:

- Total master data records.
- Active records.
- Recent changes.
- Current tenant.
- Current role.

Panels:

- Recent activity: latest loaded audit events if available.
- Getting started: load records, create a record, review activity.
- Small demo note: backend flow can be explained verbally during the mentor demo.

Do not show stack components as the main dashboard.

### 3. Master Data

Purpose: main business CRUD screen.

Features:

- Load list.
- Search/filter by code or name using current client-side data if backend search is not available.
- Load by code.
- Create record.
- Unique code helper, for example `MDP-${timestamp}` or `UI-DEMO-${timestamp}`.

Table fields:

- Code.
- Name.
- Type.
- Status.
- Updated.
- Actions if the backend supports them.

States:

- Loading.
- Empty.
- Success.
- `401 Unauthorized`: sign in again.
- `403 Forbidden`: authenticated but not allowed.
- `409 Conflict`: duplicate code for this tenant.
- `500 Unexpected`: use requestId in logs.
- Service unavailable: gateway/upstream not reachable.

Viewer behavior:

- Show a role hint: `Viewer can read records but cannot create new records.`
- The create form may be disabled, or it may submit and show the backend `403`; for demo learning, showing the real `403` is acceptable if the message is clear.

Redis note:

- If load-by-code is used for cache-aside observation, do not show a fake hit/miss badge.
- Cache behavior is proven through backend logs/metrics.

### 4. Activity Log

Purpose: user-facing activity history.

Features:

- Load activity.
- Table or timeline view.
- Empty state: `No activity for this tenant`.
- Positive empty copy for tenant 2: HTTP 200 with no tenant 1 activity means isolation is working.

Display fields:

- Time.
- Action.
- Record code.
- Type.
- Actor if available; otherwise hide it.
- Event ID in details/small text.

Copy:

```text
Activity is generated when master data changes.
```

Do not mention Kafka prominently in the UI. During the live demo, explain that this activity is backed by Kafka and `audit-log-service`.

### 5. Account

Purpose: authenticated user and local demo settings.

Show:

- Username.
- `tenant_id`.
- Roles.
- Token status: `available (hidden)`.
- API base URL preset:
  - Kong Gateway.
  - Spring Gateway legacy.
- Logout.

The API base URL belongs here rather than on the business dashboard.

## Keycloak Login Theme Concept

The Keycloak login page is separate from the React UI and remains future work.

Brand:

```text
Master Data Portal
```

Layout:

- Left panel:
  - Product tagline.
  - Simple abstract data-grid illustration.
  - Short local demo explanation.
- Right panel:
  - Standard username field.
  - Standard password field.
  - Sign in button.
  - Error message area.
  - Local account hints:
    - `tenant1-user / password` - Accountant.
    - `tenant2-user / password` - Viewer.

Future implementation path:

```text
lab-code/keycloak-lab/themes/master-data-portal/login/theme.properties
lab-code/keycloak-lab/themes/master-data-portal/login/login.ftl
lab-code/keycloak-lab/themes/master-data-portal/login/resources/css/styles.css
```

Notes:

- The Keycloak theme should visually match the React UI.
- It must not change auth logic.
- Keep the public SPA client, PKCE/local redirect settings and backend JWT validation unchanged.

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
| Teal | `#0891B2` | Activity/accent |
| Success | `#16A34A` | Success state |
| Warning | `#D97706` | Warning state |
| Danger | `#DC2626` | Error/destructive state |

### Typography

Use Inter if already available; otherwise use the system font stack.

| Role | Size | Weight | Usage |
|---|---:|---|---|
| Display | 32 | Bold | Product/welcome headers |
| Heading | 22 | Semi Bold | Page titles |
| Subheading | 18 | Semi Bold | Card titles |
| Body | 15 | Regular | Main UI text |
| Table/body small | 13 | Regular | Tables and dense panels |
| Meta | 12 | Medium | Badges, labels, timestamps |

### Layout

- Desktop frame: 1440px wide.
- Sidebar: around 248px.
- Topbar: around 72-76px.
- Content gutter: around 36-44px.
- Card radius: 8-12px.
- Compact cards and tables.

## Implementation Phases

### Phase A: Product Shell

- Replace architecture-console language with `Master Data Portal`.
- Sidebar nav: Dashboard, Master Data, Activity Log, Account.
- Move API base URL and auth metadata into Account/topbar.

### Phase B: Dashboard

- Business overview cards.
- Recent activity preview.
- No stack component dashboard.

### Phase C: Master Data

- List, search/filter, load by code and create.
- Preserve current API helpers and requestId/status display.
- Keep `401`, `403`, `409`, service unavailable and empty states clear.

### Phase D: Activity Log

- Rename Audit Events UI to Activity Log.
- Keep actual endpoint `/api/audit-events`.
- Show empty HTTP 200 as a valid tenant isolation state.

### Phase E: Keycloak Login Theme

- Implement only after React UI direction is approved.
- Do not change auth logic or backend contracts.

## What Is Intentionally Not Included

- React Native or Expo.
- Heavy UI framework.
- Redux/Zustand.
- Realtime dashboards.
- Drag/drop or advanced animations.
- Direct UI calls to PostgreSQL, Redis, Kafka, MinIO, Prometheus, Grafana or Kafka UI for business flow.
- Backend API contract changes.
- Production-grade frontend security hardening.

## Open Questions And Risks

- Figma still needs a new pass to replace the old architecture-console framing.
- Current React implementation may still contain old console labels until the next coding task.
- Keycloak custom theme needs careful container/theme mounting later.
- If status probes are added later, keep them optional and local-only.

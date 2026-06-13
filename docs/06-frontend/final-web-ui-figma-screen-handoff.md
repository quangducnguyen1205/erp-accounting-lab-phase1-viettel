# Final Web UI Figma Screen Handoff

## Status

Existing Figma file:

[AI Knowledge Workspace - Phase 1.5 Ops Console](https://www.figma.com/design/kyPvlljW1KhQhoUvqLcjco)

Current status on 2026-06-13:

- The previous Figma design-system page exists, but it still reflects the old architecture-console direction.
- The product direction has changed to `Master Data Portal`.
- The Figma Starter MCP tool-call limit is currently blocking metadata inspection, screen creation, file rename and screenshot export.
- This document and [final-web-ui-design-plan.md](final-web-ui-design-plan.md) are the source of truth until the next Figma pass.

## Product Direction

The UI should be a normal SaaS-style business web app:

```text
Master Data Portal
```

It is not an architecture console. Backend technologies are still real and demoable, but they should be explained verbally and in docs rather than becoming the main product navigation.

## Pages And Frames To Create Next

### `00 Design System`

Keep or revise the existing design-system page, but update labels and examples to the new product name.

Components to include:

- sidebar nav item;
- topbar;
- status badge;
- role badge;
- tenant badge;
- requestId chip;
- API status card;
- table;
- form field;
- primary/secondary/danger buttons;
- alert/toast;
- empty state.

### `01 Product Screens`

Frames to create:

1. `01 Welcome - Logged Out`
2. `02 Dashboard - Business Overview`
3. `03 Master Data - Accountant Success`
4. `04 Master Data - Error States`
5. `05 Activity Log - Tenant 1`
6. `06 Activity Log - Tenant 2 Empty`
7. `07 Account - Auth And Gateway Settings`
8. `08 Demo Flow Notes`

### `02 Keycloak Theme Concept`

Frames to create:

1. `01 Keycloak Login - Default`
2. `02 Keycloak Login - Error`
3. `03 Keycloak Theme Handoff`

## Frame Notes

### Welcome - Logged Out

- Title: `Master Data Portal`.
- Subtitle: `Manage shared business data across tenants`.
- Primary action: `Sign in`.
- Demo users:
  - `tenant1-user / password` - Accountant.
  - `tenant2-user / password` - Viewer.
- Note: tokens are never displayed.

### Dashboard - Business Overview

Show business metrics, not stack cards:

- total master data records;
- active records;
- recent changes;
- current tenant;
- current role;
- recent activity preview.

### Master Data

Show the core business task:

- search/filter;
- load list;
- load by code;
- create record;
- table with Code, Name, Type, Status, Updated;
- requestId/status near actions;
- duplicate `409`, forbidden `403`, missing auth `401`, unavailable and unexpected error states.

### Activity Log

Use user-facing language:

- `Activity Log`, not `Kafka Events`.
- Empty state: `No activity for this tenant`.
- Positive tenant isolation copy for tenant 2.
- Event details can include event ID in small text.

### Account

Show:

- username;
- `tenant_id`;
- roles;
- token status: `available (hidden)`;
- API base URL preset: Kong Gateway and Spring Gateway legacy;
- logout.

### Demo Flow Notes

Keep backend explanation secondary:

```text
Sign in
-> Create master data
-> Activity appears
-> Mentor can inspect Kafka UI and Loki outside the product UI
-> Tenant2 isolation
-> Viewer create returns 403
```

## Screenshot Gallery

Screenshots were not exported because the Figma MCP tool-call limit blocked screen creation and export.

Expected screenshot directory:

```text
docs/06-frontend/images/final-ui-design/
```

Expected screenshot filenames after the next Figma pass:

```text
01-welcome-logged-out.png
02-dashboard-business-overview.png
03-master-data-accountant-success.png
04-master-data-error-states.png
05-activity-log-tenant1.png
06-activity-log-tenant2-empty.png
07-account-auth-gateway-settings.png
08-demo-flow-notes.png
09-keycloak-login-default.png
10-keycloak-login-error.png
11-keycloak-theme-handoff.png
```

The folder should stay in Git with `.gitkeep`; generated PNGs should be added only after checking for secrets/tokens/private data.

## Design Decisions

### Sidebar And Topbar Layout

- Sidebar: Dashboard, Master Data, Activity Log, Account.
- Topbar: product context, current tenant/role, login/logout status.
- Avoid stack cards as primary UI.

### Role And Tenant Visibility

The UI may display:

- username;
- `tenant_id`;
- role badge;
- token state as `available (hidden)`.

Backend services remain responsible for JWT validation, role checks and tenant isolation.

### RequestId And Status Visibility

Every API action should show:

- HTTP status;
- endpoint/action;
- requestId;
- short explanation for `401`, `403`, `409`, `500` and unavailable states.

### Observability Is Outside The Product Flow

The product UI may include small demo notes or links, but it should not call Grafana/Loki/Kafka UI APIs.

The demo script can explain:

- activity is backed by Kafka and `audit-log-service`;
- logs are inspected in Grafana Loki;
- Kafka transport is inspected in Kafka UI.

### Keycloak Theme Is Separate From React UI

Future theme path:

```text
lab-code/keycloak-lab/themes/master-data-portal/login/theme.properties
lab-code/keycloak-lab/themes/master-data-portal/login/login.ftl
lab-code/keycloak-lab/themes/master-data-portal/login/resources/css/styles.css
```

Do not change auth logic when implementing the theme.

## Implementation Mapping

The next React task should adjust the already-created component structure:

```text
lab-code/web-ui-demo/src/
  App.jsx
  components/
    AppShell.jsx
    Sidebar.jsx
    Topbar.jsx
    StatusCard.jsx
    Badge.jsx
    RequestStatus.jsx
    DataTable.jsx
    EmptyState.jsx
    Alert.jsx
  screens/
    WelcomeScreen.jsx
    DashboardScreen.jsx
    MasterDataScreen.jsx
    AuditEventsScreen.jsx
    ObservabilityScreen.jsx
```

Recommended rename/mapping:

| Current concept | Product concept |
|---|---|
| Audit Events | Activity Log |
| Observability | Account secondary demo/help notes, not primary nav |
| Architecture dashboard | Business dashboard |
| AI Knowledge Workspace | Master Data Portal |

Current API helpers remain usable:

| UI screen | Existing helper |
|---|---|
| Master Data list | `loadMasterData(apiBaseUrl)` |
| Load by code | `loadMasterDataByCode(code, apiBaseUrl)` |
| Create master data | `createMasterData(payload, apiBaseUrl)` |
| Activity Log | `loadAuditEvents(apiBaseUrl, limit)` |

No backend API contract change is required.

## Approved For Coding Next

The next coding task should implement the product redesign:

1. Replace old brand/title text with `Master Data Portal`.
2. Change primary nav to Dashboard, Master Data, Activity Log, Account.
3. Replace architecture dashboard with business overview.
4. Move gateway/API settings into Account.
5. Keep requestId/status/error handling.
6. Keep Kong as default API path.
7. Keep Keycloak theme as future work.

## Future Work

- Rename/update Figma file when MCP limit allows.
- Generate product-focused Figma frames and screenshots.
- Implement Keycloak custom login theme.
- Optional final visual QA after the React redesign.
